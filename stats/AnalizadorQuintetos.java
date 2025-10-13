package stats;

import java.util.*;

/**
 * Clase para analizar el play-by-play de un equipo de baloncesto y calcular
 * estadísticas de quintetos utilizando la clase Quinteto existente.
 * Incluye logs de depuración para identificar por qué no se crean quintetos.
 * 
 * Mejoras:
 * - Logs detallados para depurar (remover después de probar).
 * - Inicialización robusta: Recolecta hasta 20 acciones iniciales; fallback si >=3 jugadores.
 * - Detección ampliada de inicios de cuarto y sustituciones.
 * - Crea quinteto incluso si size !=5 después de subs (ajusta si >5, loguea warning).
 * - Solo procesa stats si hay al menos 4 jugadores para robustez.
 */
public class AnalizadorQuintetos {

    /**
     * Analiza todas las acciones de un equipo y devuelve un mapa de estadísticas
     * por quinteto (clave = Quinteto.generarClave(), valor = Quinteto).
     */
    public Map<String, Quinteto> analizarQuintetos(Equipo equipo) {
        Accion[] acciones = equipo.getAcciones();
        System.out.println("DEBUG: Número total de acciones: " + (acciones != null ? acciones.length : 0)); // LOG

        if (acciones == null || acciones.length == 0) {
            System.out.println("DEBUG: Acciones null o vacías. No se procesa nada."); // LOG
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

        for (Accion a : acciones) {
            if (a == null) {
                System.out.println("DEBUG: Acción null, skip."); // LOG
                continue;
            }
            if (a.getAccion() == null) {
                System.out.println("DEBUG: Acción sin getAccion(), skip."); // LOG
                continue;
            }

            String accionStr = a.getAccion().toLowerCase();
            double tiempoGlobal = calcularTiempoGlobal(a);
            Jug jugador = a.getJugador();
            //System.out.println("DEBUG: Procesando acción: '" + accionStr + "' por jugador: " + (jugador != null ? jugador.getNombre() : "null") + " en tiempo: " + tiempoGlobal); // LOG

            // --- Manejo de cambio de cuarto ---
            if (esInicioCuarto(accionStr)) {
                System.out.println("DEBUG: Detectado inicio de cuarto: " + a.getCuarto()); // LOG
                // Finalizar quinteto anterior si existe
                if (quintetoEnCurso != null) {
                    quintetoEnCurso.setTiempoFin(tiempoGlobal);
                    System.out.println("DEBUG: Finalizado quinteto anterior en tiempo: " + tiempoGlobal); // LOG
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
                continue;
            }

            // Si no hay jugador, skip para stats/subs (pero podría ser inicio de cuarto sin jugador)
            if (jugador == null) {
                System.out.println("DEBUG: Sin jugador, skip para stats/subs."); // LOG
                continue;
            }

            // --- Inicialización automática del quinteto al inicio del cuarto ---
            if (!quintetoInicializado) {
                if (!esSustitucion(accionStr)) {
                    contadorAccionesIniciales++;
                    //System.out.println("DEBUG: Recolectando jugador inicial #" + quintetoActual.size() + ": " + jugador.getNombre() + " (acción #" + contadorAccionesIniciales + ")"); // LOG
                    if (quintetoActual.add(jugador)) { // Ignora duplicados
                        System.out.println("DEBUG: Agregado nuevo jugador. Total: " + quintetoActual.size()); // LOG
                        if (quintetoActual.size() == 5) {
                            System.out.println("DEBUG: Quinteto inicial completo! Creando Quinteto."); // LOG
                            // Crear el quinteto inicial
                            claveActual = generarClave(quintetoActual, cuartoActual);
                            resultado.putIfAbsent(claveActual, new Quinteto(cuartoActual, new HashSet<>(quintetoActual)));
                            quintetoEnCurso = resultado.get(claveActual);
                            quintetoEnCurso.setTiempoInicio(tiempoUltimoCambio);
                            quintetoInicializado = true;
                            // Procesar esta acción
                            quintetoEnCurso.agregarAccion(a);
                            tiempoUltimoCambio = tiempoGlobal;
                            System.out.println("DEBUG: Quinteto inicial creado con clave: " + claveActual + ". Mapa size: " + resultado.size()); // LOG
                        } else if (contadorAccionesIniciales >= 20 && quintetoActual.size() >= 3) {
                            // Fallback: Crea con lo que haya si pasa muchas acciones sin 5
                            System.out.println("WARNING: No se recolectaron 5 jugadores iniciales después de 20 acciones. Creando con " + quintetoActual.size() + " jugadores."); // LOG
                            claveActual = generarClave(quintetoActual, cuartoActual);
                            resultado.putIfAbsent(claveActual, new Quinteto(cuartoActual, new HashSet<>(quintetoActual)));
                            quintetoEnCurso = resultado.get(claveActual);
                            quintetoEnCurso.setTiempoInicio(tiempoUltimoCambio);
                            quintetoInicializado = true;
                            tiempoUltimoCambio = tiempoGlobal;
                            System.out.println("DEBUG: Quinteto fallback creado. Mapa size: " + resultado.size()); // LOG
                        }
                    }
                    if (!quintetoInicializado) continue; // Aún recolectando
                } else {
                    System.out.println("DEBUG: Sustitución temprana ignorada hasta inicializar quinteto."); // LOG
                    continue;
                }
            }

            // --- Manejo de sustituciones ---
            if (esSustitucion(accionStr)) {
                System.out.println("DEBUG: Detectada sustitución: " + accionStr); // LOG
                enSecuenciaSubs = true;

                // Finalizar el quinteto anterior si existe
                if (quintetoEnCurso != null) {
                    quintetoEnCurso.setTiempoFin(tiempoGlobal);
                    System.out.println("DEBUG: Finalizado quinteto antes de sub."); // LOG
                    quintetoEnCurso = null;
                    claveActual = null;
                }

                // Actualizar quinteto actual
                if (accionStr.contains("entra") || accionStr.contains("sube")) {
                    if (quintetoActual.size() < 5) {
                        quintetoActual.add(jugador);
                    } else {
                        System.out.println("WARNING: Quinteto ya tiene 5, ignorando entrada extra."); // LOG
                    }
                } else if (accionStr.contains("sale") || accionStr.contains("baja")) {
                    quintetoActual.remove(jugador);
                }
                System.out.println("DEBUG: Quinteto actual size después de sub: " + quintetoActual.size()); // LOG

                tiempoUltimaSub = tiempoGlobal;
                tiempoUltimoCambio = tiempoGlobal;
                continue;
            }

            // --- Procesar acciones no-sustitución (stats) ---
            //System.out.println("DEBUG: Procesando stats para acción no-sub."); // LOG
            if (quintetoActual.size() >= 4 && !enSecuenciaSubs) { // Relajado a >=4 para robustez
                // Si estábamos en secuencia de subs, crear nuevo quinteto
                if (enSecuenciaSubs) {
                    System.out.println("DEBUG: Fin de secuencia subs. Creando nuevo Quinteto con size: " + quintetoActual.size()); // LOG
                    if (quintetoActual.size() != 5) {
                        System.out.println("WARNING: Size !=5 después de subs (" + quintetoActual.size() + "). Ajustando..."); // LOG
                        // Opcional: Remover extras si >5 (ejemplo simple: remover uno arbitrario)
                        if (quintetoActual.size() > 5) {
                            quintetoActual.remove(quintetoActual.iterator().next());
                            System.out.println("DEBUG: Removido jugador extra. Nuevo size: " + quintetoActual.size()); // LOG
                        }
                        // Si <4, no crear (skip)
                        if (quintetoActual.size() < 4) {
                            System.out.println("WARNING: Size <4 después de subs. Skip creación."); // LOG
                            enSecuenciaSubs = false;
                            continue;
                        }
                    }
                    claveActual = generarClave(quintetoActual, cuartoActual);
                    resultado.putIfAbsent(claveActual, new Quinteto(cuartoActual, new HashSet<>(quintetoActual)));
                    quintetoEnCurso = resultado.get(claveActual);
                    quintetoEnCurso.setTiempoInicio(tiempoUltimaSub);
                    enSecuenciaSubs = false;
                    System.out.println("DEBUG: Nuevo Quinteto creado con clave: " + claveActual + ". Mapa size: " + resultado.size()); // LOG
                }

                // Agregar la acción si hay quinteto
                if (quintetoEnCurso != null) {
                    quintetoEnCurso.agregarAccion(a);
                    tiempoUltimoCambio = tiempoGlobal;
                    System.out.println("DEBUG: Stats agregadas. Puntos actuales: " + quintetoEnCurso.getPuntos()); // LOG
                }
            } else {
                System.out.println("DEBUG: Skip stats - size: " + quintetoActual.size() + ", enSecuenciaSubs: " + enSecuenciaSubs); // LOG
                enSecuenciaSubs = false; // Reset si no válido
            }
        }

        // Finalizar el último quinteto
        if (quintetoEnCurso != null && quintetoEnCurso.getTiempoFin() == 0.0) {
            double ultimoTiempo = acciones.length > 0 ? calcularTiempoGlobal(acciones[acciones.length - 1]) : tiempoUltimoCambio;
            quintetoEnCurso.setTiempoFin(ultimoTiempo);
            System.out.println("DEBUG: Finalizado último quinteto en: " + ultimoTiempo); // LOG
        }

        System.out.println("DEBUG: Análisis completado. Número de quintetos creados: " + resultado.size()); // LOG FINAL
        if (resultado.isEmpty()) {
            System.out.println("WARNING: Mapa vacío al final. Revisa los logs arriba para causas (e.g., no jugadores, no coincidencias en strings)."); // LOG
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
     * Calcula el tiempo global de la acción (prioriza tiempoGlobal, fallback a tiempo).
     */
    private double calcularTiempoGlobal(Accion a) {
        if (a.getTiempoGlobal() != 0) {
            return a.getTiempoGlobal();
        }
        if (a.getTiempo() != null) {
            return a.getTiempo().getTime() / 60000.0; // Asumiendo ms a minutos
        }
        return 0.0;
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
