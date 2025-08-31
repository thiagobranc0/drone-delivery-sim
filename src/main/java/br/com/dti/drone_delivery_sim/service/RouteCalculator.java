package br.com.dti.drone_delivery_sim.service;

import br.com.dti.drone_delivery_sim.model.NoFlyZone;
import br.com.dti.drone_delivery_sim.model.Order;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RouteCalculator {
    public static final double[] BASE = new double[]{0,0};

    private final List<NoFlyZone> zonas = new ArrayList<>();

    public List<NoFlyZone> listarZonas(){ return new ArrayList<>(zonas); }
    public void limparZonas(){ zonas.clear(); }
    public void adicionarZona(NoFlyZone z){ zonas.add(z); }

    public record ResultadoRota(List<double[]> rota, double distanciaKm){}

    public ResultadoRota vizinhoMaisProximo(List<Order> pedidos){
        List<double[]> caminho = new ArrayList<>();
        caminho.add(BASE.clone());

        double[] atual = BASE.clone();
        List<Order> restantes = new ArrayList<>(pedidos);

        while (!restantes.isEmpty()) {
            final double[] origem = atual;

            Order prox = restantes.stream()
                    .min(Comparator.comparingDouble(p -> dist(origem, new double[]{ p.getX(), p.getY() })))
                    .orElseThrow();

            atual = new double[]{ prox.getX(), prox.getY() };
            caminho.add(atual);
            restantes.remove(prox);
        }

        caminho.add(BASE.clone());

        List<double[]> ajustada = ajustarPorZonas(caminho);
        double total = 0.0;
        for (int i = 1; i < ajustada.size(); i++) total += dist(ajustada.get(i-1), ajustada.get(i));
        return new ResultadoRota(ajustada, total);
    }

    private List<double[]> ajustarPorZonas(List<double[]> path){
        if (zonas.isEmpty() || path.size() < 2) return path;

        List<double[]> out = new ArrayList<>();
        out.add(path.get(0));

        for (int i = 1; i < path.size(); i++) {
            double[] a = out.get(out.size()-1);
            double[] b = path.get(i);

            NoFlyZone hit = primeiraInter(a,b);
            if (hit == null) { out.add(b); continue; }

            double[][] cantos = new double[][]{
                    {hit.getMinX(), hit.getMinY()},{hit.getMaxX(), hit.getMinY()},
                    {hit.getMaxX(), hit.getMaxY()},{hit.getMinX(), hit.getMaxY()}
            };
            double[] pertoA = cantos[0]; double best = dist2(a, pertoA);
            for (int k=1;k<cantos.length;k++){ double d=dist2(a,cantos[k]); if (d<best){best=d; pertoA=cantos[k];}}
            if (!(pertoA[0]==a[0] && pertoA[1]==a[1])) out.add(pertoA);

            if (primeiraInter(out.get(out.size()-1), b) != null){
                double[] pertoB = cantos[0]; best = dist2(b, pertoB);
                for (int k=1;k<cantos.length;k++){ double d=dist2(b,cantos[k]); if (d<best){best=d; pertoB=cantos[k];}}
                if (!(pertoB[0]==out.get(out.size()-1)[0] && pertoB[1]==out.get(out.size()-1)[1])) out.add(pertoB);
            }
            out.add(b);
        }
        return out;
    }

    private NoFlyZone primeiraInter(double[] a, double[] b){
        for (NoFlyZone z : zonas) if (z.intersectaSegmento(a,b)) return z;
        return null;
    }

    private static double dist(double[] p, double[] q){ return Math.hypot(p[0]-q[0], p[1]-q[1]); }
    private static double dist2(double[] p, double[] q){ double dx=p[0]-q[0], dy=p[1]-q[1]; return dx*dx+dy*dy; }
}
