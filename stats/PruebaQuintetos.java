package stats;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PruebaQuintetos {

    public static void imprimirQuintetos(Map<String, Quinteto> quintetos) {
        if (quintetos == null || quintetos.isEmpty()) {
            System.out.println("No hay quintetos para mostrar.");
            return;
        }

        for (Map.Entry<String, Quinteto> entry : quintetos.entrySet()) {
            Quinteto q = entry.getValue();
            System.out.println("Clave: " + entry.getKey());
            System.out.println("Cuarto: " + q.getCuarto());
            Set<Jug> jugadoresSet = q.getJugadores();
            List<String> nombres = new ArrayList<>();
            for (Jug j : jugadoresSet) {
                nombres.add(j.getNombre());
            }
            System.out.println("Jugadores: " + String.join(", ", nombres));
            System.out.println("Tiempo Inicio: " + q.getTiempoInicio() + " min");
            System.out.println("Tiempo Fin: " + q.getTiempoFin() + " min");
            System.out.println("Minutos Jugados: " + q.getMinutosJugados());
            System.out.println("Puntos: " + q.getPuntos() + " | T2: " + q.getT2met() + "/" + q.getT2int() + " | T3: " + q.getT3met() + "/" + q.getT3int() + " | TL: " + q.getTlmet() + "/" + q.getTlint());
            System.out.println("Reb Of/Def: " + q.getRebOf() + "/" + q.getRebDef() + " | Pérdidas: " + q.getPerdidas());
            System.out.println("---------------------------------------------------");
        }
    }
}
