package stats;

import java.util.*;

/**
 * Clase para analizar el play-by-play de un equipo de baloncesto y calcular
 * estadísticas de quintetos utilizando la clase Quinteto existente.
 * 
 * Mejoras:
 * - Inicialización robusta: Recolecta jugadores iniciales hasta exactamente 5 (sin fallback <5).
 * - Detección ampliada de inicios de cuarto y sustituciones.
 * - Quintetos estrictos: Solo crea quintetos si size == 5 exactamente (no >=4 ni ajustes automáticos).
 * - Si después de subs size !=5, no crea nuevo quinteto (mantiene el anterior y loguea warning).
 * - Manejo de secuencias de sustituciones: Actualiza quintetoActual durante subs consecutivas,
 *   pero solo crea/finaliza el nuevo Quinteto al final de la secuencia (cuando llega una acción no-sub).
 *   Si la secuencia termina al final del cuarto/loop sin acción no-sub, crea el quinteto si size==5.
 * - Tiempo global corregido: Usa enfoque secuencial basado en el orden de acciones para evitar negativos.
 *   - Si getTiempoGlobal() !=0, usa directo (de LeerExcel).
 *   - Fallback: Asigna tiempo progresivo (index * DELTA_TIEMPO), donde DELTA_TIEMPO = 0.1 min por acción
 *     (ajustable; asume ~10 acciones por minuto en play-by-play). Esto asegura tiempoFin > tiempoInicio.
 *   - Ignora getTiempo() Date si causa epoch grandes/negativos; enfócate en orden cronológico.
 *   - Para cuartos: Reinicia contador secuencial por cuarto si detectado.
 */
public class AnalizadorQuintetos {

    private static final double DELTA_TIEMPO_POR_ACCION = 0.1; // Minutos por acción (ajusta basado en tu data: ~0.05-0.2)

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
        int contadorAccionesIniciales = 0; // Para limitar recolección inicial
        double tiempoGlobalBase = 0.0; // Contador secuencial global (reinicia por cuarto)
        int indexAccionGlobal = 0; // Índice secuencial para fallback

        for (Accion a : acciones) {
            if (a == null) {
                //System.out.println("DEBUG: Acción null, skip."); // LOG
                continue;
            }
            if (a.getAccion() == null) {
                //System.out.println("DEBUG: Acción sin getAccion(), skip."); // LOG
                continue;
            }

            String accionStr = a.getAccion().toLowerCase();
            double tiempoGlobal = calcularTiempoGlobal(a, indexAccionGlobal, tiempoGlobalBase); // Secuencial con base
            indexAccionGlobal++; // Incrementar para siguiente
            Jug jugador = a.getJugador();
            //System.out.println("DEBUG: Procesando acción: '" + accionStr + "' por jugador: " + (jugador != null ? jugador.getNombre() : "null") + " en tiempo: " + tiempoGlobal); // LOG

            // --- Manejo de cambio de cuarto ---
            if (esInicioCuarto(accionStr)) {
                //System.out.println("DEBUG: Detectado inicio de cuarto: " + a.getCuarto()); // LOG
                // Finalizar quinteto anterior si existe (incluyendo si en secuencia de subs pendiente)
                if (quintetoEnCurso != null) {
                    quintetoEnCurso.setTiempoFin(tiempoGlobal);
                    //System.out.println("DEBUG: Finalizado quinteto anterior en tiempo: " + tiempoGlobal); // LOG
                    quintetoEnCurso = null;
                    claveActual = null;
                }
                if (enSecuenciaSubs) {
                    // Si hay secuencia pendiente al cambio de cuarto, crear el quinteto intermedio solo si size==5
                    if (quintetoActual.size() == 5) {
                        claveActual = generarClave(quintetoActual, cuartoActual);
                        resultado.putIfAbsent(claveActual, new Quinteto(cuartoActual, new HashSet<>(quintetoActual)));
                        quintetoEnCurso = resultado.get(claveActual);
                        quintetoEnCurso.setTiempoInicio(tiempoUltimaSub);
                        quintetoEnCurso.setTiempoFin(tiempoGlobal); // Sin stats, pero registrado
                        //System.out.println("DEBUG: Quinteto post-subs creado al cambio de cuarto. Clave: " + claveActual); // LOG
                    } else {
                        //System.out.println("WARNING: Secuencia de subs pendiente al cambio de cuarto, pero size !=5 (" + quintetoActual.size() + "). No se crea quinteto."); // LOG
                    }
                    enSecuenciaSubs = false;
                }
                // Reiniciar para nuevo cuarto
                cuartoActual = a.getCuarto();
                quintetoActual.clear();
                quintetoEnCurso = null;
                claveActual = null;
                tiempoUltimoCambio = tiempoGlobal;
                tiempoUltimaSub = tiempoGlobal;
                enSecuenciaSubs = false;
                quintetoInicializado = false;
                contadorAccionesIniciales = 0;
                tiempoGlobalBase = tiempoGlobal; // Reiniciar base secuencial por cuarto
                continue;
            }

            // Si no hay jugador, skip para stats/subs (pero podría ser inicio de cuarto sin jugador)
            if (jugador == null) {
                //System.out.println("DEBUG: Sin jugador, skip para stats/subs."); // LOG
                continue;
            }

            // --- Inicialización automática del quinteto al inicio del cuarto ---
            if (!quintetoInicializado) {
                if (!esSustitucion(accionStr)) {
                    contadorAccionesIniciales++;
                    //System.out.println("DEBUG: Recolectando jugador inicial #" + quintetoActual.size() + ": " + jugador.getNombre() + " (acción #" + contadorAccionesIniciales + ")"); // LOG
                    if (quintetoActual.add(jugador)) { // Ignora duplicados
                        //System.out.println("DEBUG: Agregado nuevo jugador. Total: " + quintetoActual.size()); // LOG
                        if (quintetoActual.size() == 5) {
                            //System.out.println("DEBUG: Quinteto inicial completo! Creando Quinteto."); // LOG
                            // Crear el quinteto inicial
                            claveActual = generarClave(quintetoActual, cuartoActual);
                            resultado.putIfAbsent(claveActual, new Quinteto(cuartoActual, new HashSet<>(quintetoActual)));
                            quintetoEnCurso = resultado.get(claveActual);
                            quintetoEnCurso.setTiempoInicio(tiempoUltimoCambio);
                            quintetoInicializado = true;
                            // Procesar esta acción
                            quintetoEnCurso.agregarAccion(a);
                            tiempoUltimoCambio = tiempoGlobal;
                            //System.out.println("DEBUG: Quinteto inicial creado con clave: " + claveActual + ". Mapa size: " + resultado.size()); // LOG
                        } else if (contadorAccionesIniciales >= 50) { // Aumentado límite para esperar más acciones
                            //System.out.println("WARNING: No se recolectaron exactamente 5 jugadores iniciales después de 50 acciones. Esperando más o skip inicial."); // LOG
                            // No crear fallback; continuar recolectando o fallar silenciosamente
                        }
                    }
                    if (!quintetoInicializado) continue; // Aún recolectando
                } else {
                    //System.out.println("DEBUG: Sustitución temprana ignorada hasta inicializar quinteto."); // LOG
                    continue;
                }
            }

            // --- Manejo de sustituciones (secuencia consecutiva) ---
            if (esSustitucion(accionStr)) {
                //System.out.println("DEBUG: Detectada sustitución: " + accionStr); // LOG

                // Si es la PRIMERA sub de la secuencia, finalizar el quinteto anterior
                if (!enSecuenciaSubs) {
                    if (quintetoEnCurso != null) {
                        quintetoEnCurso.setTiempoFin(tiempoGlobal);
                        //System.out.println("DEBUG: Finalizado quinteto antes de inicio de secuencia subs."); // LOG
                        quintetoEnCurso = null;
                        claveActual = null;
                    }
                    enSecuenciaSubs = true;
                }

                // Actualizar quinteto actual (para todas las subs de la secuencia)
                boolean cambioRealizado = false;
                if (accionStr.contains("entra") || accionStr.contains("sube")) {
                    if (quintetoActual.size() < 5) {
                        quintetoActual.add(jugador);
                        cambioRealizado = true;
                    } else {
                        //System.out.println("WARNING: Quinteto ya tiene 5, ignorando entrada extra."); // LOG
                    }
                } else if (accionStr.contains("sale") || accionStr.contains("baja")) {
                    if (quintetoActual.remove(jugador)) {
                        cambioRealizado = true;
                    }
                }
                if (cambioRealizado) {
                    //System.out.println("DEBUG: Quinteto actual size después de sub: " + quintetoActual.size()); // LOG
                }

                tiempoUltimaSub = tiempoGlobal;
                tiempoUltimoCambio = tiempoGlobal;
                continue; // No procesar stats en subs
            }

            // --- Procesar acciones no-sustitución (stats): Fin de secuencia de subs ---
            //System.out.println("DEBUG: Procesando stats para acción no-sub."); // LOG
            if (quintetoActual.size() == 5) { // Estricto: Solo si exactamente 5
                // Si estábamos en secuencia de subs, finalizarla y crear nuevo quinteto
                if (enSecuenciaSubs) {
                    //System.out.println("DEBUG: Fin de secuencia subs. Creando nuevo Quinteto con size: " + quintetoActual.size()); // LOG
                    claveActual = generarClave(quintetoActual, cuartoActual);
                    resultado.putIfAbsent(claveActual, new Quinteto(cuartoActual, new HashSet<>(quintetoActual)));
                    quintetoEnCurso = resultado.get(claveActual);
                    quintetoEnCurso.setTiempoInicio(tiempoUltimaSub);
                    enSecuenciaSubs = false;
                    //System.out.println("DEBUG: Nuevo Quinteto creado con clave: " + claveActual + ". Mapa size: " + resultado.size()); // LOG
                }

                // Agregar la acción si hay quinteto
                if (quintetoEnCurso != null) {
                    quintetoEnCurso.agregarAccion(a);
                    tiempoUltimoCambio = tiempoGlobal;
                    //System.out.println("DEBUG: Stats agregadas. Puntos actuales: " + quintetoEnCurso.getPuntos()); // LOG
                }
            } else {
                //System.out.println("DEBUG: Skip stats - size !=5: " + quintetoActual.size() + ", enSecuenciaSubs: " + enSecuenciaSubs); // LOG
                if (enSecuenciaSubs) {
                    // Si fin de secuencia pero size !=5, no crear y resetear (mantener quinteto anterior si existe)
                    //System.out.println("WARNING: Fin de secuencia subs, pero size !=5 (" + quintetoActual.size() + "). No se crea nuevo quinteto."); // LOG
                    enSecuenciaSubs = false;
                }
            }
        }

        // Finalizar el último quinteto (incluyendo si en secuencia pendiente al final del loop)
        if (quintetoEnCurso != null && quintetoEnCurso.getTiempoFin() == 0.0) {
            double ultimoTiempo = indexAccionGlobal * DELTA_TIEMPO_POR_ACCION; // Último tiempo secuencial
            quintetoEnCurso.setTiempoFin(ultimoTiempo);
            //System.out.println("DEBUG: Finalizado último quinteto en: " + ultimoTiempo); // LOG
        }
        if (enSecuenciaSubs) {
            // Si termina el loop con secuencia de subs pendiente, crear el quinteto final solo si size==5
            if (quintetoActual.size() == 5) {
                double ultimoTiempo = indexAccionGlobal * DELTA_TIEMPO_POR_ACCION;
                claveActual = generarClave(quintetoActual, cuartoActual);
                resultado.putIfAbsent(claveActual, new Quinteto(cuartoActual, new HashSet<>(quintetoActual)));
                quintetoEnCurso = resultado.get(claveActual);
                quintetoEnCurso.setTiempoInicio(tiempoUltimaSub);
                quintetoEnCurso.setTiempoFin(ultimoTiempo);
                //System.out.println("DEBUG: Quinteto final post-subs creado al final del loop. Clave: " + claveActual); // LOG
            } else {
                //System.out.println("WARNING: Secuencia de subs al final del loop, pero size !=5 (" + quintetoActual.size() + "). No se crea quinteto final."); // LOG
            }
            enSecuenciaSubs = false;
        }

        //System.out.println("DEBUG: Análisis completado. Número de quintetos creados: " + resultado.size()); // LOG FINAL
        if (resultado.isEmpty()) {
            //System.out.println("WARNING: Mapa vacío al final. Revisa los logs arriba para causas (e.g., no jugadores, no coincidencias en strings)."); // LOG
        }
        return resultado;
    }

    /**
     * Determina si la acción es inicio de cuarto (más patrones para robustez).
     */
    private boolean esInicioCuarto(String accion) {
        return accion.contains("comienzo del cuarto") || accion.contains("inicio del cuarto") ||
               accion.contains("1er cuarto") || accion.contains("primer cuarto") ||
               accion.contains("segundo cuarto") || accion.contains("tercer cuarto") ||
               accion.contains("cuarto cuarto") || accion.contains("fin del cuarto") ||
               accion.contains("start of quarter") || accion.contains("end of quarter"); // En caso de inglés
    }

    /**
     * Determina si la acción es una sustitución (más patrones para robustez).
     */
    private boolean esSustitucion(String accion) {
        return accion.contains("sustitución") || accion.contains("entra") || accion.contains("sale") ||
               accion.contains("sube") || accion.contains("baja") || accion.contains("sustituye") ||
               accion.contains("substitution") || accion.contains("enters") || accion.contains("exits"); // En caso de inglés
    }

    /**
     * Calcula el tiempo global de la acción (secuencial para evitar negativos).
     * - Si getTiempoGlobal() !=0, usa directo (de LeerExcel, si válido y positivo).
     * - Fallback secuencial: tiempoGlobal = tiempoGlobalBase + (index * DELTA_TIEMPO_POR_ACCION).
     *   - index: Índice de la acción (progresivo).
     *   - tiempoGlobalBase: Reiniciado al inicio de cada cuarto (0 para primer cuarto, acumula).
     * - Ignora getTiempo() Date (causa epoch grandes/negativos); enfócate en orden cronológico.
     * - Asegura tiempoGlobal >= tiempoGlobalBase (no negativos en getMinutosJugados = fin - inicio).
     * - Ajusta DELTA_TIEMPO_POR_ACCION basado en tu data (e.g., si partido de 48 min, total ~480 acciones -> 0.1 min/acción).
     */
    private double calcularTiempoGlobal(Accion a, int indexAccion, double tiempoGlobalBase) {
        if (a.getTiempoGlobal() != 0 && a.getTiempoGlobal() > 0) { // Solo si positivo y válido
            return a.getTiempoGlobal();
        }

        // Fallback secuencial: Progresivo desde base
        double tiempoSecuencial = tiempoGlobalBase + (indexAccion * DELTA_TIEMPO_POR_ACCION);

        // Clamp para evitar negativos (aunque secuencial no debería)
        return Math.max(tiempoGlobalBase, tiempoSecuencial);
    }

    /**
     * Genera clave para un set de jugadores (usando nombres) y cuarto.
     */
    private String generarClave(Set<Jug> jugadores, int cuarto) {
        List<String> lista = new ArrayList<>();
        for (Jug j : jugadores) {
            if (j != null) {
                lista.add(j.getNombre());
            }
        }
        Collections.sort(lista);
        return cuarto + " - " + String.join(", ", lista);
    }
}
