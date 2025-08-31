package br.com.dti.drone_delivery_sim.service;

import br.com.dti.drone_delivery_sim.model.Drone;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class BatterySimulator {

    public enum Policy { STRICT, SMART }

    public record ResultadoBateria(List<double[]> rota, double distanciaKm, boolean viavel, int paradasRecarga){}

    public ResultadoBateria aplicar(Policy politica, Drone drone, List<double[]> rota){
        if (rota == null || rota.size() < 2) return new ResultadoBateria(rota, 0.0, true, 0);

        final double alcance = drone.getAlcanceKm();
        double restante = alcance, total = 0.0;
        int paradas = 0;

        List<double[]> out = new ArrayList<>();
        out.add(rota.get(0));

        for (int i = 1; i < rota.size(); i++) {
            double[] a = out.get(out.size()-1);
            double[] b = rota.get(i);
            double seg = dist(a,b);

            if (seg <= restante) {
                out.add(b); restante -= seg; total += seg; continue;
            }

            if (politica == Policy.STRICT) {
                return new ResultadoBateria(rota, total + seg + resto(rota, i), false, paradas);
            }

            double volta = dist(a, RouteCalculator.BASE);
            if (volta > restante) return new ResultadoBateria(rota, total + volta + resto(rota, i), false, paradas);

            out.add(RouteCalculator.BASE.clone());
            total += volta; paradas++; restante = alcance;

            double segDaBase = dist(RouteCalculator.BASE, b);
            if (segDaBase > restante) return new ResultadoBateria(rota, total + segDaBase + resto(rota, i), false, paradas);

            out.add(b);
            total += segDaBase; restante -= segDaBase;
        }
        return new ResultadoBateria(out, total, true, paradas);
    }

    private static double resto(List<double[]> rota, int i){
        double t=0.0; for (int k=i; k<rota.size()-1; k++) t += dist(rota.get(k), rota.get(k+1)); return t;
    }
    private static double dist(double[] p, double[] q){ return Math.hypot(p[0]-q[0], p[1]-q[1]); }
}

