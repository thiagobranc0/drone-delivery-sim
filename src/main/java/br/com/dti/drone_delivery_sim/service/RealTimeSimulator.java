package br.com.dti.drone_delivery_sim.service;

import br.com.dti.drone_delivery_sim.enums.DroneState;
import br.com.dti.drone_delivery_sim.model.Delivery;
import br.com.dti.drone_delivery_sim.model.Drone;
import br.com.dti.drone_delivery_sim.model.Order;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Simulador em tempo real (in-memory).
 * - Continua gerando dashboard (como já fazia).
 * - Agora também: filas de "missões" por drone, tick(segundos), telemetria e estados dinâmicos.
 */
@Service
public class RealTimeSimulator {

    private List<Delivery> ultimoPlano = new ArrayList<>();
    private List<Order> pedidosUsados = new ArrayList<>();

    private final DroneService drones;
    private final Map<String, Deque<Missao>> filasPorDrone = new LinkedHashMap<>();
    private final Map<String, Telemetria> telemetrias = new LinkedHashMap<>();

    private volatile boolean autoLigado = false;
    private ScheduledExecutorService scheduler;
    private long tickMillis = 1000L; // default 1s em modo AUTO

    private static final double VELOCIDADE_MIN_KMH = 1.0; // evita zero
    private static final int PAUSA_ENTREGA_SEC = 10;      // tempo parado em "DELIVERING"
    private static final int PAUSA_RECARGA_SEC = 20;      // tempo parado em "CHARGING" em pit-stop
    private static final double TAXA_RECARGA_PCT_POR_SEC = 0.5; // 0.5% por segundo de pausa

    public RealTimeSimulator(DroneService drones) {
        this.drones = drones;
    }

    public void registrarPlano(List<Delivery> entregas, List<Order> pedidos) {
        this.ultimoPlano = new ArrayList<>(entregas);
        this.pedidosUsados = new ArrayList<>(pedidos);
    }

    public record Relatorio(int quantidadeEntregas, double tempoMedioMin, String droneMaisEficiente, String mapaAscii){}

    public Relatorio gerarRelatorio() {
        if (ultimoPlano.isEmpty()) return new Relatorio(0,0.0,"-", "(sem dados)");

        int qtd = ultimoPlano.stream().mapToInt(d -> d.getPedidosIds().size()).sum();
        double media = ultimoPlano.stream().mapToDouble(Delivery::getEtaMin).average().orElse(0.0);
        String melhorDrone = ultimoPlano.stream()
                .collect(Collectors.groupingBy(Delivery::getDroneId, Collectors.summingDouble(Delivery::getDistanciaKm)))
                .entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse("-");

        return new Relatorio(qtd, round2(media), melhorDrone, mapaAscii());
    }

    private String mapaAscii(){
        if (pedidosUsados.isEmpty()) return "(sem pedidos)";

        double maxX = pedidosUsados.stream().mapToDouble(Order::getX).map(Math::abs).max().orElse(5);
        double maxY = pedidosUsados.stream().mapToDouble(Order::getY).map(Math::abs).max().orElse(5);
        int w = (int)Math.max(20, Math.ceil(maxX)*2 + 3);
        int h = (int)Math.max(10, Math.ceil(maxY)*2 + 3);

        char[][] g = new char[h][w];
        for (char[] row : g) Arrays.fill(row, '.');

        int cx = w/2, cy = h/2;
        g[cy][cx] = 'B';

        for (Order o : pedidosUsados){
            int px = cx + (int)Math.round(o.getX());
            int py = cy - (int)Math.round(o.getY());
            if (px>=0 && px<w && py>=0 && py<h) g[py][px] = '*';
        }

        StringBuilder sb = new StringBuilder();
        for (char[] row : g) sb.append(row).append('\n');
        return sb.toString();
    }

    /** Converte o último plano registrado em filas de missões (um deque por drone). */
    public synchronized void carregarPlanoComoMissoesDoUltimoPlano() {
        if (ultimoPlano.isEmpty()) throw new IllegalStateException("não há plano registrado - execute /plan antes");
        // limpa filas antigas
        filasPorDrone.clear();
        telemetrias.clear();

        // agrupa por drone
        Map<String, List<Delivery>> porDrone = ultimoPlano.stream()
                .collect(Collectors.groupingBy(Delivery::getDroneId, LinkedHashMap::new, Collectors.toList()));

        for (var e : porDrone.entrySet()) {
            String id = e.getKey();
            Deque<Missao> fila = new ArrayDeque<>();
            for (Delivery d : e.getValue()) {
                fila.add(new Missao(d.getDroneId(), d.getPedidosIds(), deepCopyRota(d.getRota())));
            }
            filasPorDrone.put(id, fila);
            telemetrias.put(id, Telemetria.inicial(id));
        }
    }

    /** Liga modo AUTO (scheduler) ou apenas configura o modo MANUAL (sem threads). */
    public synchronized void iniciar(String modo, Long tickMillisParam) {
        if ("AUTO".equalsIgnoreCase(modo)) {
            if (tickMillisParam != null && tickMillisParam > 0) this.tickMillis = tickMillisParam;
            if (scheduler != null) scheduler.shutdownNow();
            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "realtime-sim");
                t.setDaemon(true);
                return t;
            });
            autoLigado = true;
            scheduler.scheduleAtFixedRate(() -> {
                try { tick(1); } catch (Exception ignored) {}
            }, 0, this.tickMillis, TimeUnit.MILLISECONDS);
        } else {
            autoLigado = false; // modo MANUAL
            if (scheduler != null) { scheduler.shutdownNow(); scheduler = null; }
        }
    }

    public synchronized void parar() {
        autoLigado = false;
        if (scheduler != null) { scheduler.shutdownNow(); scheduler = null; }
    }

    /** Avança a simulação 'segundos' segundos (uso no modo MANUAL e também pelo AUTO). */
    public synchronized void tick(long segundos) {
        if (segundos <= 0) return;

        // inicia missão para drones ociosos que tenham fila
        for (String id : filasPorDrone.keySet()) {
            var tel = telemetrias.computeIfAbsent(id, Telemetria::inicial);
            if (!tel.emMissao && !filasPorDrone.get(id).isEmpty()) {
                var m = filasPorDrone.get(id).peekFirst();
                tel.iniciarMissao(m);
                atualizarEstadoDrone(id, DroneState.FLYING);
            }
        }

        // avança cada drone
        for (String id : telemetrias.keySet()) {
            var tel = telemetrias.get(id);
            var dOpt = drones.buscar(id);
            if (dOpt.isEmpty()) continue;
            Drone drone = dOpt.get();

            if (!tel.emMissao) continue;

            double velKmh = Math.max(drone.getVelocidadeKmh(), VELOCIDADE_MIN_KMH);
            double distAvancar = (velKmh / 3600.0) * segundos; // unidades de rota são km

            // pausas (entrega/recarga)
            if (tel.pausaRestanteSec > 0) {
                long consumoPausa = Math.min(tel.pausaRestanteSec, segundos);
                tel.pausaRestanteSec -= consumoPausa;
                // recarga durante pausa
                if (tel.estado == DroneState.CHARGING) {
                    tel.bateriaPercent = Math.min(100.0, tel.bateriaPercent + TAXA_RECARGA_PCT_POR_SEC * consumoPausa);
                }
                if (tel.pausaRestanteSec > 0) continue; // ainda em pausa
                // saiu da pausa -> volta a voar se ainda não terminou
                if (!tel.chegouAoFimDaMissao()) {
                    tel.estado = DroneState.FLYING;
                    atualizarEstadoDrone(id, DroneState.FLYING);
                }
            }

            // consumir distância pelos segmentos
            while (distAvancar > 0 && tel.emMissao) {
                double[] a = tel.rota.get(tel.segmentoIdx);
                double[] b = tel.rota.get(tel.segmentoIdx + 1);
                double segLen = dist(a, b);
                double restoSeg = segLen - tel.percorridoNoSegmentoKm;

                if (distAvancar >= restoSeg - 1e-9) {
                    // chega ao próximo waypoint
                    distAvancar -= restoSeg;
                    tel.posX = b[0]; tel.posY = b[1];
                    tel.bateriaPercent = Math.max(0, tel.bateriaPercent - drone.getConsumoPercentPorKm() * restoSeg);
                    tel.segmentoIdx += 1;
                    tel.percorridoNoSegmentoKm = 0.0;

                    if (tel.segmentoIdx >= tel.rota.size() - 1) {
                        // missão concluída (chegou ao último ponto)
                        tel.finalizarMissao();
                        atualizarEstadoDrone(id, DroneState.IDLE);
                        // remove da fila
                        var fila = filasPorDrone.getOrDefault(id, new ArrayDeque<>());
                        if (!fila.isEmpty()) fila.removeFirst();
                        break;
                    } else {
                        // chegou num waypoint intermediário
                        boolean emBase = (Math.abs(tel.posX) < 1e-9 && Math.abs(tel.posY) < 1e-9);
                        boolean pontoEntrega = tel.isPontoDeEntregaAtual();

                        if (pontoEntrega) {
                            tel.estado = DroneState.DELIVERING;
                            atualizarEstadoDrone(id, DroneState.DELIVERING);
                            tel.pausaRestanteSec = PAUSA_ENTREGA_SEC;
                            break;
                        } else if (emBase && !tel.inicioDaRota()) {
                            // pit-stop na base (rota SMART pode inserir base no meio)
                            tel.estado = DroneState.CHARGING;
                            atualizarEstadoDrone(id, DroneState.CHARGING);
                            tel.pausaRestanteSec = PAUSA_RECARGA_SEC;
                            break;
                        } else {
                            // continua voando para o próximo segmento
                            tel.estado = DroneState.FLYING;
                            atualizarEstadoDrone(id, DroneState.FLYING);
                        }
                    }
                } else {
                    // avança dentro do segmento
                    double fraq = (tel.percorridoNoSegmentoKm + distAvancar) / segLen;
                    fraq = Math.min(1.0, fraq);
                    tel.posX = a[0] + (b[0] - a[0]) * fraq;
                    tel.posY = a[1] + (b[1] - a[1]) * fraq;
                    tel.percorridoNoSegmentoKm += distAvancar;
                    tel.bateriaPercent = Math.max(0, tel.bateriaPercent - drone.getConsumoPercentPorKm() * distAvancar);
                    distAvancar = 0;
                }
            }

            // estado RETURNING quando o próximo waypoint é a base final
            if (tel.emMissao && tel.proximoWaypointEhBaseFinal()) {
                tel.estado = DroneState.RETURNING;
                atualizarEstadoDrone(id, DroneState.RETURNING);
            }
        }
    }

    public synchronized Status status() {
        int ativos = (int) telemetrias.values().stream().filter(t -> t.emMissao).count();
        int ociosos = Math.max(0, drones.listar().size() - ativos);
        Map<String,Integer> pend = new LinkedHashMap<>();
        for (var e : filasPorDrone.entrySet()) pend.put(e.getKey(), e.getValue().size());
        return new Status(autoLigado ? "AUTO" : "MANUAL", ativos, ociosos, tickMillis, pend);
    }

    public synchronized List<TelemetryDTO> listarTelemetria() {
        return telemetrias.values().stream().map(TelemetryDTO::from).toList();
    }

    public synchronized TelemetryDTO telemetriaDoDrone(String id) {
        Telemetria t = telemetrias.get(id);
        if (t == null) return new TelemetryDTO(id, "UNKNOWN", 0,0, 0, false, 0, List.of());
        return TelemetryDTO.from(t);
    }

    // ---------- tipos auxiliares ----------

    public record Status(String modo, int dronesAtivos, int dronesOciosos, long tickMillis, Map<String,Integer> missoesPendentesPorDrone) {}
    public record TelemetryDTO(String droneId, String estado, double posX, double posY,
                               double bateriaPercent, boolean emMissao, int proximoWaypointIdx,
                               List<Long> pedidosIds) {
        static TelemetryDTO from(Telemetria t){
            return new TelemetryDTO(t.droneId, t.estado.name(), round2(t.posX), round2(t.posY),
                    round2(t.bateriaPercent), t.emMissao, Math.min(t.segmentoIdx+1, t.rota.size()-1),
                    t.pedidosIds);
        }
    }

    private static class Telemetria {
        final String droneId;
        DroneState estado = DroneState.IDLE;
        boolean emMissao = false;
        List<double[]> rota = List.of();
        List<Long> pedidosIds = List.of();
        int segmentoIdx = 0;               // índice do segmento atual (entre rota[i] e rota[i+1])
        double percorridoNoSegmentoKm = 0; // progresso no segmento atual
        double posX = 0, posY = 0;
        double bateriaPercent = 100.0;
        long pausaRestanteSec = 0;

        Telemetria(String droneId){ this.droneId = droneId; }

        static Telemetria inicial(String id){ return new Telemetria(id); }

        void iniciarMissao(Missao m){
            this.emMissao = true;
            this.estado = DroneState.FLYING;
            this.rota = m.rota;
            this.pedidosIds = m.pedidosIds;
            this.segmentoIdx = 0;
            this.percorridoNoSegmentoKm = 0;
            this.pausaRestanteSec = 0;
            this.posX = rota.get(0)[0];
            this.posY = rota.get(0)[1];
        }

        boolean chegouAoFimDaMissao(){ return segmentoIdx >= rota.size() - 1; }

        boolean proximoWaypointEhBaseFinal(){
            if (segmentoIdx + 1 == rota.size() - 1) {
                double[] next = rota.get(segmentoIdx + 1);
                return Math.abs(next[0]) < 1e-9 && Math.abs(next[1]) < 1e-9;
            }
            return false;
        }

        boolean inicioDaRota(){ return segmentoIdx == 0; }

        boolean isPontoDeEntregaAtual(){
            // considera como ponto de entrega qualquer waypoint que coincida com algum pedido desta missão
            if (segmentoIdx < 0 || segmentoIdx >= rota.size()) return false;
            double[] p = rota.get(segmentoIdx);
            for (long pid : pedidosIds) {
                // nota: precisamos de pedidosUsados (do último plano) para achar coords; fallback: ignora
            }

            // Como heurística simples para MVP: se não for (0,0), consideramos entrega
            // e evitamos em sequência usando a pausa curta de entrega.
            return !(Math.abs(p[0]) < 1e-9 && Math.abs(p[1]) < 1e-9);
        }

        void finalizarMissao(){
            this.emMissao = false;
            this.estado = DroneState.IDLE;
            this.rota = List.of();
            this.pedidosIds = List.of();
            this.segmentoIdx = 0;
            this.percorridoNoSegmentoKm = 0;
            this.pausaRestanteSec = 0;
            this.posX = 0; this.posY = 0;
        }
    }

    private static class Missao {
        final String droneId;
        final List<Long> pedidosIds;
        final List<double[]> rota;

        Missao(String droneId, List<Long> pedidosIds, List<double[]> rota) {
            this.droneId = droneId;
            this.pedidosIds = new ArrayList<>(pedidosIds);
            this.rota = rota;
        }
    }

    // ---------- util ----------
    private static List<double[]> deepCopyRota(List<double[]> rota){
        List<double[]> out = new ArrayList<>(rota.size());
        for (double[] p : rota) out.add(new double[]{p[0], p[1]});
        return out;
    }

    private static double round2(double v){ return Math.round(v*100.0)/100.0; }

    private static double dist(double[] a, double[] b){
        double dx = a[0]-b[0], dy = a[1]-b[1];
        return Math.sqrt(dx*dx + dy*dy);
    }

    private void atualizarEstadoDrone(String id, DroneState estado){
        var d = drones.buscar(id);
        d.ifPresent(dr -> dr.setEstado(estado));
    }
}
