package stats;

import java.util.*;

public class AnalizadorQuintetos {

    /** Estructura para las estadísticas de cada quinteto */
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

        /**
         * Devuelve una clave única del quinteto: cuarto + nombres ordenados
         */
        public String generarClave() {
            java.util.List<String> lista = new java.util.ArrayList<String>(jugadores);
            java.util.Collections.sort(lista);
            StringBuilder sb = new StringBuilder();
            sb.append(cuarto).append(" - ");
            for (int i = 0; i < lista.size(); i++) {
                sb.append(lista.get(i));
                if (i < lista.size() - 1) sb.append(", ");
            }
            return sb.toString();
        }
    }

    /**
     * Analiza las acciones de un equipo y devuelve un mapa con estadísticas de cada quinteto
     */
    public Map<String, QuintetoStats> analizarQuintetos(Equipo equipo) {
        Accion[] acciones = equipo.getAcciones(); // Usamos el array directamente
        Map<String, QuintetoStats> resultado = new LinkedHashMap<>();

        Set<String> quintetoActual = new HashSet<>();
        int cuartoActual = 1;
        double tiempoUltimoCambio = 0.0;

        for (Accion a : acciones) {
            String accion = a.getAccion().toLowerCase();
            double tiempoGlobal = a.getTiempoGlobal(); // Debe existir este campo en Accion

            // Cambio de cuarto
            if (accion.contains("comienzo del cuarto")) {
                cuartoActual = a.getCuarto();
                quintetoActual.clear();
                tiempoUltimoCambio = tiempoGlobal;
                continue;
            }

            // Sustituciones
            if (accion.contains("sustitución")) {
                if (accion.contains("entra a pista")) quintetoActual.add(a.getJugador().getNombre());
                else if (accion.contains("sale de pista")) quintetoActual.remove(a.getJugador().getNombre());

                if (quintetoActual.size() == 5) {
                    QuintetoStats q = new QuintetoStats();
                    q.cuarto = cuartoActual;
                    q.jugadores.addAll(quintetoActual);
                    q.tiempoInicio = tiempoUltimoCambio;
                    q.tiempoFin = tiempoGlobal;

                    resultado.put(q.generarClave(), q);
                }

                tiempoUltimoCambio = tiempoGlobal;
                continue;
            }

            // Solo procesar si hay 5 jugadores
            if (quintetoActual.size() != 5) continue;

            String clave = generarClave(quintetoActual, cuartoActual);
            resultado.putIfAbsent(clave, new QuintetoStats());
            QuintetoStats q = resultado.get(clave);
            q.cuarto = cuartoActual;
            q.jugadores.addAll(quintetoActual);
            if (q.tiempoInicio == 0) q.tiempoInicio = tiempoUltimoCambio;

            // Estadísticas
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


    /**
     * Genera clave externa si no quieres usar el método dentro de QuintetoStats
     */
    private String generarClave(Set<String> jugadores, int cuarto) {
        java.util.List<String> lista = new java.util.ArrayList<String>(jugadores);
        java.util.Collections.sort(lista);
        return cuarto + " - " + String.join(", ", lista);
    }
}
