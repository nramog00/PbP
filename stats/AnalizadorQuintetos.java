package stats;

import java.util.*;
import java.util.List;
import java.util.ArrayList;

public class AnalizadorQuintetos {

    /** Clase interna para almacenar estadísticas de un quinteto */
    public static class QuintetoStats {
        public int cuarto;
        public Set<String> jugadores = new HashSet<>();

        public int puntos = 0;
        public int t2met = 0;
        public int t2int = 0;
        public int t3met = 0;
        public int t3int = 0;
        public int tlmet = 0;
        public int tlint = 0;
        public int rebOf = 0;
        public int rebDef = 0;
        public int perdidas = 0;

        public double tiempoInicio = 0;
        public double tiempoFin = 0;

        public double getMinutosJugados() {
            return tiempoFin - tiempoInicio;
        }

        public String generarClave() {
            List<String> lista = new ArrayList<>(jugadores);
            Collections.sort(lista);
            return cuarto + " - " + String.join(", ", lista);
        }
    }

    /** Analiza todas las acciones de un equipo y devuelve estadísticas de cada quinteto */
    public Map<String, QuintetoStats> analizarQuintetos(Equipo equipo) {
        Accion[] acciones = equipo.getAcciones();
        Map<String, QuintetoStats> resultado = new LinkedHashMap<>();
        if (acciones == null || acciones.length == 0) return resultado;

        Set<String> quintetoActual = new HashSet<>();
        int cuartoActual = 1;
        double tiempoUltimoCambio = 0.0;

        for (Accion a : acciones) {
            if (a == null || a.getJugador() == null || a.getAccion() == null) continue;

            String accion = a.getAccion().toLowerCase();
            double tiempoGlobal = (a.getTiempoGlobal() != 0) ? a.getTiempoGlobal() : a.getTiempo().getTime() / 60000.0;

            // --- Cambio de cuarto ---
            if (accion.contains("comienzo del cuarto")) {
                cuartoActual = a.getCuarto();
                quintetoActual.clear();
                tiempoUltimoCambio = tiempoGlobal;
                continue;
            }

            // --- Sustituciones ---
            if (accion.contains("sustitución")) {
                String jugador = a.getJugador().getNombre();
                if (accion.contains("entra")) quintetoActual.add(jugador);
                else if (accion.contains("sale")) quintetoActual.remove(jugador);

                // Guardar quinteto si ya hay 5 jugadores
                if (quintetoActual.size() == 5) {
                    String clave = generarClave(quintetoActual, cuartoActual);
                    resultado.putIfAbsent(clave, new QuintetoStats());
                    QuintetoStats q = resultado.get(clave);
                    q.cuarto = cuartoActual;
                    q.jugadores.addAll(quintetoActual);
                    q.tiempoInicio = tiempoUltimoCambio;
                    q.tiempoFin = tiempoGlobal;
                }

                tiempoUltimoCambio = tiempoGlobal;
                continue;
            }

            // --- Inicializar quinteto automáticamente al comienzo ---
            if (quintetoActual.size() < 5) {
                quintetoActual.add(a.getJugador().getNombre());
                if (quintetoActual.size() < 5) continue; // esperar a tener 5 jugadores
            }

            // --- Procesar quinteto existente ---
            String clave = generarClave(quintetoActual, cuartoActual);
            resultado.putIfAbsent(clave, new QuintetoStats());
            QuintetoStats q = resultado.get(clave);
            q.cuarto = cuartoActual;
            q.jugadores.addAll(quintetoActual);
            if (q.tiempoInicio == 0) q.tiempoInicio = tiempoUltimoCambio;

            // --- Estadísticas ---
            if (accion.contains("tiro de 2")) { 
                q.t2int++; 
                if (accion.contains("anotado")) { q.t2met++; q.puntos += 2; } 
            } else if (accion.contains("tiro de 3")) { 
                q.t3int++; 
                if (accion.contains("anotado")) { q.t3met++; q.puntos += 3; } 
            } else if (accion.contains("tiro libre")) { 
                q.tlint++; 
                if (accion.contains("anotado")) { q.tlmet++; q.puntos += 1; } 
            } else if (accion.contains("rebote ofensivo")) q.rebOf++;
            else if (accion.contains("rebote defensivo")) q.rebDef++;
            else if (accion.contains("pérdida") || accion.contains("perdida")) q.perdidas++;

            q.tiempoFin = tiempoGlobal;
            tiempoUltimoCambio = tiempoGlobal;
        }

        return resultado;
    }

    /** Genera clave externa si no quieres usar el método dentro de QuintetoStats */
    private String generarClave(Set<String> jugadores, int cuarto) {
        List<String> lista = new ArrayList<>(jugadores);
        Collections.sort(lista);
        return cuarto + " - " + String.join(", ", lista);
    }
}
