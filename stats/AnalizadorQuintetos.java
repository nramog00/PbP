package stats;

import java.util.*;
import java.text.*;

public class AnalizadorQuintetos {

    /**
     * Analiza todas las acciones de un equipo y devuelve un mapa con las estadísticas por quinteto.
     */
    public Map<String, Quinteto> analizarQuintetos(Equipo equipo) {
        Accion[] acciones = equipo.getAcciones();
        if (acciones == null || acciones.length == 0) return new LinkedHashMap<>();

        Map<String, Quinteto> resultado = new LinkedHashMap<>();
        Set<Jug> quintetoActual = new HashSet<>();

        Quinteto quintetoEnCurso = null;
        String claveActual = null;
        int cuartoActual = 1;
        double tiempoUltimo = 0.0;

        for (Accion a : acciones) {
            if (a == null || a.getAccion() == null) continue;

            String accion = a.getAccion().toLowerCase();
            double tiempoGlobal = obtenerTiempoEnMinutos(a);
            Jug jugador = a.getJugador();

            // --- Inicio de nuevo cuarto ---
            if (esInicioCuarto(accion)) {
                if (quintetoEnCurso != null) {
                    quintetoEnCurso.setTiempoFin(tiempoGlobal);
                }
                cuartoActual = (a.getCuarto() > 0) ? a.getCuarto() : (cuartoActual + 1);
                quintetoActual.clear();
                quintetoEnCurso = null;
                claveActual = null;
                tiempoUltimo = tiempoGlobal;
                continue;
            }

            // --- Sustituciones ---
            if (esSustitucion(accion)) {
                if (accion.contains("entra")) quintetoActual.add(jugador);
                else if (accion.contains("sale")) quintetoActual.remove(jugador);

                if (quintetoActual.size() == 5) {
                    if (quintetoEnCurso != null) quintetoEnCurso.setTiempoFin(tiempoGlobal);
                    claveActual = generarClave(quintetoActual, cuartoActual);
                    resultado.putIfAbsent(claveActual, new Quinteto(cuartoActual, new HashSet<>(quintetoActual)));
                    quintetoEnCurso = resultado.get(claveActual);
                    quintetoEnCurso.setTiempoInicio(tiempoGlobal);
                }
                continue;
            }

            // --- Acciones estadísticas ---
            if (quintetoActual.size() == 5) {
                if (quintetoEnCurso == null) {
                    claveActual = generarClave(quintetoActual, cuartoActual);
                    resultado.putIfAbsent(claveActual, new Quinteto(cuartoActual, new HashSet<>(quintetoActual)));
                    quintetoEnCurso = resultado.get(claveActual);
                    quintetoEnCurso.setTiempoInicio(tiempoUltimo);
                }
                quintetoEnCurso.agregarAccion(a);
                quintetoEnCurso.setTiempoFin(tiempoGlobal);
            }

            tiempoUltimo = tiempoGlobal;
        }

        return resultado;
    }

    /** Detecta inicios de cuarto en texto */
    private boolean esInicioCuarto(String accion) {
        return accion.contains("comienzo del cuarto") ||
               accion.contains("inicio del cuarto") ||
               accion.contains("empieza el cuarto") ||
               accion.contains("start of quarter");
    }

    /** Detecta sustituciones */
    private boolean esSustitucion(String accion) {
        return accion.contains("sustitución") || accion.contains("entra") || accion.contains("sale") ||
               accion.contains("substitution") || accion.contains("enters");
    }

    /** Convierte HH:MM (quedando en el cuarto) a minutos totales transcurridos */
    private double obtenerTiempoEnMinutos(Accion a) {
        try {
            String tiempo = a.getTiempoS(); // Ej: "09:56"
            if (tiempo == null || tiempo.isEmpty()) return a.getTiempoGlobal();
            String[] partes = tiempo.split(":");
            int min = Integer.parseInt(partes[0]);
            int seg = Integer.parseInt(partes[1]);
            double transcurrido = (10 - min) + (60 - seg) / 60.0; // suponiendo cuartos de 10min
            return (a.getCuarto() - 1) * 10 + transcurrido;
        } catch (Exception e) {
            return a.getTiempoGlobal();
        }
    }

    /** Clave única de quinteto */
    private String generarClave(Set<Jug> jugadores, int cuarto) {
        List<String> lista = new ArrayList<>();
        for (Jug j : jugadores) lista.add(j.getNombre());
        Collections.sort(lista);
        return cuarto + " - " + String.join(", ", lista);
    }
}
