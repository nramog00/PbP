package stats;

import java.util.Map;

public class PruebaQuintetos {

    public static void imprimirQuintetos(Map<String, AnalizadorQuintetos.QuintetoStats> quintetos) {
        if (quintetos == null || quintetos.isEmpty()) {
            System.out.println("No hay quintetos para mostrar.");
            return;
        }

        for (Map.Entry<String, AnalizadorQuintetos.QuintetoStats> entry : quintetos.entrySet()) {
            AnalizadorQuintetos.QuintetoStats q = entry.getValue();
            System.out.println("Clave: " + entry.getKey());
            System.out.println("Cuarto: " + q.cuarto);
            System.out.println("Jugadores: " + String.join(", ", q.jugadores));
            System.out.println("Tiempo Inicio: " + q.tiempoInicio + " min");
            System.out.println("Tiempo Fin: " + q.tiempoFin + " min");
            System.out.println("Minutos Jugados: " + q.getMinutosJugados());
            System.out.println("Puntos: " + q.puntos + " | T2: " + q.t2met + "/" + q.t2int + " | T3: " + q.t3met + "/" + q.t3int + " | TL: " + q.tlmet + "/" + q.tlint);
            System.out.println("Reb Of/Def: " + q.rebOf + "/" + q.rebDef + " | Pérdidas: " + q.perdidas);
            System.out.println("---------------------------------------------------");
        }
    }
}
