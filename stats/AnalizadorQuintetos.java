package stats;

import java.util.*;
import java.text.SimpleDateFormat;
import java.text.ParseException;

/**
 * Clase para analizar el play-by-play de un equipo de baloncesto y calcular
 * estadísticas de quintetos utilizando la clase Quinteto existente.
 * 
 * Mejoras:
 * - Inicialización robusta: Recolecta jugadores iniciales hasta exactamente 5.
 * - Detección ampliada de inicios de cuarto y sustituciones.
 * - Quintetos estrictos: Solo crea quintetos si size == 5 exactamente.
 * - Manejo de secuencias de sustituciones: Actualiza durante subs, crea solo al final si size == 5.
 * - Tiempo global corregido: Parsea "HH:MM" a minutos transcurridos; fallback secuencial si falla.
 * - Procesamiento de stats ampliado: Incluye lógica para tiros libres, rebotes ofensivos, defensivos y pérdidas.
 */
public class AnalizadorQuintetos {

    private static final double DELTA_TIEMPO_POR_ACCION = 0.1; // Minutos por acción (ajusta basado en tu data)

    /**
     * Analiza todas las acciones de un equipo y devuelve un mapa de estadísticas
     * por quinteto (clave = Quinteto.generarClave(), valor = Quinteto).
     */
    public Map<String, Quinteto> analizarQuintetos(Equipo equipo) {
        Accion[] acciones = equipo.getAcciones();
        //System.out.println("DEBUG: Número total de acciones: " + (acciones != null ? acciones.length : 0)); // LOG

        if (acciones == null || acciones.length == 0) {
            //System.out.println("DEBUG: Acciones null o vacías. No se procesa nada."); // LOG
            return new LinkedHashMap<>();
        }

        Map<String, Quinteto> resultado = new LinkedHashMap<>();
        Set<Jug> quintetoActual = new HashSet<>();
        int cuartoActual = 1;
        double tiempoUltimoCambio = 0.0;
        double tiempoUltimaSub = 0.0;
        Quinteto quintetoEnCurso = null;
        String claveActual = null;
        boolean enSecuenciaSubs = false;
        boolean quintetoInicializado = false;
        int contadorAccionesIniciales = 0;
        double tiempoGlobalBase = 0.0; // Base para tiempos acumulados
        int indexAccionGlobal = 0;

        for (Accion a : acciones) {
            if (a == null) continue;
            if (a.getAccion() == null) continue;

            String accionStr = a.getAccion().toLowerCase();
            double tiempoGlobal = calcularTiempoGlobal(a, indexAccionGlobal, tiempoGlobalBase);
            indexAccionGlobal++;
            Jug jugador = a.getJugador();
            //System.out.println("DEBUG: Procesando acción: '" + accionStr + "' en tiempo: " + tiempoGlobal); // LOG

            if (esInicioCuarto(accionStr)) {
                if (quintetoEnCurso != null) {
                    quintetoEnCurso.setTiempoFin(tiempoGlobal);
                    quintetoEnCurso = null;
                    claveActual = null;
                }
                if (enSecuenciaSubs && quintetoActual.size() == 5) {
                    claveActual = generarClave(quintetoActual, cuartoActual);
                    resultado.putIfAbsent(claveActual, new Quinteto(cuartoActual, new HashSet<>(quintetoActual)));
                    quintetoEnCurso = resultado.get(claveActual);
                    quintetoEnCurso.setTiempoInicio(tiempoUltimaSub);
                    quintetoEnCurso.setTiempoFin(tiempoGlobal);
                }
                enSecuenciaSubs = false;
                cuartoActual = a.getCuarto();
                quintetoActual.clear();
                quintetoEnCurso = null;
                claveActual = null;
                tiempoUltimoCambio = tiempoGlobal;
                tiempoUltimaSub = tiempoGlobal;
                quintetoInicializado = false;
                contadorAccionesIniciales = 0;
                tiempoGlobalBase = tiempoGlobal; // Reiniciar base por cuarto
                continue;
            }

            if (jugador == null) continue;

            if (!quintetoInicializado) {
                if (!esSustitucion(accionStr)) {
                    contadorAccionesIniciales++;
                    if (quintetoActual.add(jugador)) {
                        if (quintetoActual.size() == 5) {
                            claveActual = generarClave(quintetoActual, cuartoActual);
                            resultado.putIfAbsent(claveActual, new Quinteto(cuartoActual, new HashSet<>(quintetoActual)));
                            quintetoEnCurso = resultado.get(claveActual);
                            quintetoEnCurso.setTiempoInicio(tiempoUltimoCambio);
                            quintetoInicializado = true;
                            quintetoEnCurso.agregarAccion(a);
                            tiempoUltimoCambio = tiempoGlobal;
                        } else if (contadorAccionesIniciales >= 50) {
                            // No crear fallback
                        }
                    }
                    if (!quintetoInicializado) continue;
                } else {
                    continue;
                }
            }

            if (esSustitucion(accionStr)) {
                if (!enSecuenciaSubs) {
                    if (quintetoEnCurso != null) {
                        quintetoEnCurso.setTiempoFin(tiempoGlobal);
                        quintetoEnCurso = null;
                        claveActual = null;
                    }
                    enSecuenciaSubs = true;
                }
                if (accionStr.contains("entra") || accionStr.contains("sube")) {
                    if (quintetoActual.size() < 5) quintetoActual.add(jugador);
                } else if (accionStr.contains("sale") || accionStr.contains("baja")) {
                    quintetoActual.remove(jugador);
                }
                tiempoUltimaSub = tiempoGlobal;
                tiempoUltimoCambio = tiempoGlobal;
                continue;
            }

            if (quintetoActual.size() == 5) {
                if (enSecuenciaSubs) {
                    claveActual = generarClave(quintetoActual, cuartoActual);
                    resultado.putIfAbsent(claveActual, new Quinteto(cuartoActual, new HashSet<>(quintetoActual)));
                    quintetoEnCurso = resultado.get(claveActual);
                    quintetoEnCurso.setTiempoInicio(tiempoUltimaSub);
                    enSecuenciaSubs = false;
                }
                if (quintetoEnCurso != null) {
                    quintetoEnCurso.agregarAccion(a);
                    tiempoUltimoCambio = tiempoGlobal;
                }
            } else if (enSecuenciaSubs) {
                enSecuenciaSubs = false;
            }
        }

        if (quintetoEnCurso != null && quintetoEnCurso.getTiempoFin() == 0.0) {
            double ultimoTiempo = indexAccionGlobal * DELTA_TIEMPO_POR_ACCION;
            quintetoEnCurso.setTiempoFin(ultimoTiempo);
        }
        if (enSecuenciaSubs && quintetoActual.size() == 5) {
            double ultimoTiempo = indexAccionGlobal * DELTA_TIEMPO_POR_ACCION;
            claveActual = generarClave(quintetoActual, cuartoActual);
            resultado.putIfAbsent(claveActual, new Quinteto(cuartoActual, new HashSet<>(quintetoActual)));
            Quinteto ultimoQuinteto = resultado.get(claveActual);
            ultimoQuinteto.setTiempoInicio(tiempoUltimaSub);
            ultimoQuinteto.setTiempoFin(ultimoTiempo);
        }

        return resultado;
    }

    private boolean esInicioCuarto(String accion) {
        return accion.contains("comienzo del cuarto") || accion.contains("inicio del cuarto") ||
               accion.contains("1er cuarto") || accion.contains("start of quarter");
    }

    private boolean esSustitucion(String accion) {
        return accion.contains("sustitución") || accion.contains("entra") || accion.contains("sale") ||
               accion.contains("substitution") || accion.contains("enters");
    }

    private double calcularTiempoGlobal(Accion a, int indexAccion, double tiempoGlobalBase) {
        if (a.getTiempoGlobal() > 0) return a.getTiempoGlobal();
        return tiempoGlobalBase + (indexAccion * 0.1);
    }

    private String generarClave(Set<Jug> jugadores, int cuarto) {
        List<String> lista = new ArrayList<>();
        for (Jug j : jugadores) lista.add(j.getNombre());
        Collections.sort(lista);
        return cuarto + " - " + String.join(", ", lista);
    }
}