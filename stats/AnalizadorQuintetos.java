package stats;

import java.util.*;

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
        boolean quintetoInicializado;
        boolean enSecuenciaSubs = false;
        double tiempoUltimoCambio = 0.0;
        double tiempoUltimaSub = 0.0;
        double tiempoGlobalBase = 0.0;
        int nuevoCuarto = 1;

        for (Accion a : acciones) {
            if (a == null || a.getAccion() == null) continue;

            String accion = a.getAccion().toLowerCase();
            double tiempoGlobal = obtenerTiempoEnMinutos(a);
            Jug jugador = a.getJugador();

            //System.out.println("DEBUG acción: [" + a.getAccion() + "]");

            // --- Inicio de nuevo cuarto ---
            if (esInicioCuarto(accion)) {
                nuevoCuarto = a.getCuarto();
                double tiempoActual = tiempoGlobal;
                //System.out.println("---- INICIO CUARTO " + nuevoCuarto + " a " + tiempoActual + " min ----");

                // Cerrar el quinteto anterior si estaba activo
                if (quintetoEnCurso != null) {
                    quintetoEnCurso.setTiempoFin(tiempoActual);
                }

                // Si había un quinteto anterior con 5 jugadores, se usa como base para el nuevo cuarto
                if (quintetoActual != null && quintetoActual.size() == 5) {
                    claveActual = generarClave(quintetoActual, nuevoCuarto);
                    resultado.putIfAbsent(claveActual, new Quinteto(nuevoCuarto, new HashSet<>(quintetoActual)));
                    //System.out.println("Creando quinteto en cuarto " + cuartoActual + " -> " + claveActual);
                    quintetoEnCurso = resultado.get(claveActual);
                    quintetoEnCurso.setTiempoInicio(tiempoActual);
                    quintetoEnCurso.setTiempoFin(0.0);
                    // Marcamos como inicializado
                    quintetoInicializado = true;
                } else {
                    // Si no había quinteto previo, se limpia todo
                    if (resultado.size() > 0) {
                        Quinteto ultimo = new ArrayList<>(resultado.values()).get(resultado.size() - 1);
                        quintetoActual.addAll(ultimo.getJugadores());
                        claveActual = generarClave(quintetoActual, nuevoCuarto);
                        resultado.putIfAbsent(claveActual, new Quinteto(nuevoCuarto, new HashSet<>(quintetoActual)));
                        quintetoEnCurso = resultado.get(claveActual);
                        quintetoEnCurso.setTiempoInicio(tiempoActual);
                    } else {
                        quintetoActual.clear();
                        quintetoEnCurso = null;
                        claveActual = null;
                    }
                }

                cuartoActual = nuevoCuarto;
                //System.out.println("Acción detectada en cuarto: " + a.getCuarto() + " -> " + a.getAccion());
                enSecuenciaSubs = false;
                tiempoUltimoCambio = tiempoActual;
                tiempoUltimaSub = tiempoActual;
                tiempoGlobalBase = tiempoActual;

                continue;
            }

            // --- Sustituciones ---
            if (esSustitucion(accion)) {
                //System.out.println("Sustitución detectada: " + accion + " a " + tiempoGlobal + " min");
                if (accion.contains("entra")) quintetoActual.add(jugador);
                else if (accion.contains("sale")) quintetoActual.remove(jugador);

                if (quintetoActual.size() == 5) {
                    if (quintetoEnCurso != null) quintetoEnCurso.setTiempoFin(tiempoGlobal);
                    claveActual = generarClave(quintetoActual, cuartoActual);
                    resultado.putIfAbsent(claveActual, new Quinteto(cuartoActual, new HashSet<>(quintetoActual)));
                    quintetoEnCurso = resultado.get(claveActual);
                    quintetoEnCurso.setTiempoInicio(tiempoGlobal);
                }

                if (quintetoEnCurso != null && quintetoEnCurso.getTiempoInicio() == 0.0) {
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

            if (quintetoEnCurso != null && quintetoEnCurso.getTiempoInicio() == 0.0) {
                quintetoEnCurso.setTiempoInicio(tiempoUltimo);
            } //si no arriba

            Iterator<Map.Entry<String, Quinteto>> it = resultado.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Quinteto> e = it.next();
                Quinteto q = e.getValue();
                if (q.getTiempoFin() <= q.getTiempoInicio()) {
                    it.remove(); // eliminar quintetos con 0 minutos o negativos
                }
            }
        }

        if (quintetoEnCurso != null && quintetoEnCurso.getTiempoFin() == 0.0) {
            quintetoEnCurso.setTiempoFin(40.0); // si son 4 cuartos de 10 min
        }

        return resultado;
    }

    // Detecta inicios de cuarto en texto
    private boolean esInicioCuarto(String accion) {
        String s = accion.toLowerCase();

        // Coincidencias típicas en español e inglés
        return s.matches(".*(inicio|comienzo|empieza|comienza).*cuarto.*")
            || s.matches(".*(start).*quarter.*")
            || s.matches(".*(1er|2º|3er|4º|primer|segundo|tercer|cuarto).*cuarto.*")
            || s.matches(".*(periodo|período|period).*start.*")
            || s.matches(".*(inicio|start).*q[1-4].*");
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

    /** Calcula estadísticas del rival para cada quinteto del equipo */
    public Map<String, Quinteto> calcularEstadisticasRival(Map<String, Quinteto> equipo, Map<String, Quinteto> rival) {
        Map<String, Quinteto> resultado = new LinkedHashMap<>();

        for (Map.Entry<String, Quinteto> entry : equipo.entrySet()) {
            Quinteto q = entry.getValue();
            Quinteto rivalStats = new Quinteto(q.getCuarto(), new HashSet<>());
            double inicio = q.getTiempoInicio();
            double fin = q.getTiempoFin();
            boolean tuvoSolape = false;

            for (Quinteto rq : rival.values()) {
                if (rq.getCuarto() != q.getCuarto()) continue;

                double inicioR = rq.getTiempoInicio();
                double finR = rq.getTiempoFin();

                // Calculamos el tramo de solape real
                double inicioSolape = Math.max(inicio, inicioR);
                double finSolape = Math.min(fin, finR);

                if (finSolape > inicioSolape) {
                    double duracionSolape = finSolape - inicioSolape;
                    double duracionRival = finR - inicioR;
                    double factor = duracionRival > 0 ? duracionSolape / duracionRival : 1.0;

                    rivalStats.sumarStatsParcial(rq, factor);
                    tuvoSolape = true;
                }
            }

            // Si no hubo ningún solape, igual añadimos un rival vacío
            rivalStats.setTiempoInicio(inicio);
            rivalStats.setTiempoFin(fin);
            rivalStats.setMinutosJugados(fin - inicio);

            if (!tuvoSolape) {
                rivalStats.setCuarto(q.getCuarto());
                rivalStats.setJugadores(new HashSet<>());
            }

            resultado.put(entry.getKey(), rivalStats);
        }

        return resultado;
    }
}
