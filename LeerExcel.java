import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import stats.Accion;
import stats.Equipo;
import stats.Jug;

import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.UnitValue;


public class LeerExcel {
    private List<Equipo> equipos = new ArrayList<>();
    private List<Jug> jugadores = new ArrayList<>();

    public void leerArchivoExcel(String rutaArchivo) {
        int eqId = 0;
        int filasProcesadas = 0;
        int accionesCreadas = 0;
        try (FileInputStream fis = new FileInputStream(new File(rutaArchivo));
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet hoja = workbook.getSheetAt(0);
            System.out.println("DEBUG: Número total de filas en la hoja: " + (hoja.getLastRowNum() + 1)); // LOG

            for (Row fila : hoja) {
                // Salta la fila de headers (fila 0)
                if (fila.getRowNum() == 0) {
                    //System.out.println("DEBUG: Saltando fila de headers (fila 0)."); // LOG
                    continue;
                }

                try {
                    filasProcesadas++;
                    //System.out.println("DEBUG: Procesando fila " + fila.getRowNum() + "..."); // LOG

                    // Celda 1: Nombre del jugador (String)
                    org.apache.poi.ss.usermodel.Cell cellJugador = fila.getCell(1);
                    if (cellJugador == null || cellJugador.getCellType() != CellType.STRING) {
                        System.out.println("WARNING: Fila " + fila.getRowNum() + ": Sin nombre de jugador válido. Skip."); // LOG
                        continue;
                    }
                    String nombre = cellJugador.getStringCellValue().trim();
                    if (nombre.isEmpty()) {
                        System.out.println("WARNING: Fila " + fila.getRowNum() + ": Nombre vacío. Skip."); // LOG
                        continue;
                    }

                    // Buscar o crear jugador
                    Jug jugador = null;
                    for (Jug j : jugadores) {
                        if (j.getNombre().equalsIgnoreCase(nombre)) {
                            jugador = j;
                            break;
                        }
                    }
                    if (jugador == null) {
                        jugador = new Jug(nombre);
                        jugadores.add(jugador);
                        System.out.println("DEBUG: Creado nuevo jugador: " + nombre); // LOG
                    }

                    // Celda 0: Nombre del equipo (String)
                    org.apache.poi.ss.usermodel.Cell cellEquipo = fila.getCell(0);
                    String equipoStr = (cellEquipo != null && cellEquipo.getCellType() == CellType.STRING) ? 
                                       cellEquipo.getStringCellValue().trim() : "DESCONOCIDO";

                    // Buscar o crear equipo
                    Equipo eqObj = null;
                    for (Equipo e : this.equipos) {
                        if (e.getNombre() != null && e.getNombre().trim().equalsIgnoreCase(equipoStr)) {
                            eqObj = e;
                            break;
                        }
                    }
                    if (eqObj == null) {
                        eqObj = new Equipo(eqId++, equipoStr, equipoStr);
                        this.equipos.add(eqObj);
                        System.out.println("DEBUG: Creado nuevo equipo: " + equipoStr); // LOG
                    }

                    // Asignar equipo al jugador (bidireccional)
                    if (jugador.getEquipo() != eqObj) {
                        jugador.setEquipo(eqObj);
                        eqObj.agregarJugador(jugador);
                    }

                    // Celda 4: Cuarto (NUMERIC)
                    int cuarto = 1;
                    org.apache.poi.ss.usermodel.Cell cellCuarto = fila.getCell(4);
                    if (cellCuarto != null && cellCuarto.getCellType() == CellType.NUMERIC) {
                        cuarto = (int) cellCuarto.getNumericCellValue();
                    }

                    // Celda 7 y 8: Tanteos (NUMERIC)
                    int tanteoLocal = 0;
                    org.apache.poi.ss.usermodel.Cell cellTanteoLocal = fila.getCell(7);
                    if (cellTanteoLocal != null && cellTanteoLocal.getCellType() == CellType.NUMERIC) {
                        tanteoLocal = (int) cellTanteoLocal.getNumericCellValue();
                    }
                    int tanteoVisitante = 0;
                    org.apache.poi.ss.usermodel.Cell cellTanteoVisitante = fila.getCell(8);
                    if (cellTanteoVisitante != null && cellTanteoVisitante.getCellType() == CellType.NUMERIC) {
                        tanteoVisitante = (int) cellTanteoVisitante.getNumericCellValue();
                    }

                    // Celda 2: Acción (String o NUMERIC)
                    org.apache.poi.ss.usermodel.Cell acc = fila.getCell(2);
                    if (acc == null) {
                        System.out.println("WARNING: Fila " + fila.getRowNum() + ": Sin acción. Skip."); // LOG
                        continue;
                    }
                    String accionTexto = "";
                    if (acc.getCellType() == CellType.STRING) {
                        accionTexto = acc.getStringCellValue().trim();
                    } else if (acc.getCellType() == CellType.NUMERIC) {
                        accionTexto = String.valueOf((int) acc.getNumericCellValue());
                    }
                    if (accionTexto.isEmpty()) {
                        System.out.println("WARNING: Fila " + fila.getRowNum() + ": Acción vacía. Skip."); // LOG
                        continue;
                    }

                    // Celda 3: Tiempo (String o Date)
                    org.apache.poi.ss.usermodel.Cell ctime = fila.getCell(3);
                    Date time = null;
                    double tiempoGlobal = 0.0; // Para Accion (minutos)
                    if (ctime != null) {
                        try {
                            if (ctime.getCellType() == CellType.STRING) {
                                String timeStr = ctime.getStringCellValue().trim();
                                // Intenta múltiples formatos
                                SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                SimpleDateFormat sdf2 = new SimpleDateFormat("HH:mm:ss");
                                SimpleDateFormat sdf3 = new SimpleDateFormat("mm:ss");
                                try {
                                    time = sdf1.parse(timeStr);
                                } catch (Exception e1) {
                                    try {
                                        time = sdf2.parse(timeStr);
                                    } catch (Exception e2) {
                                        try {
                                            time = sdf3.parse(timeStr);
                                        } catch (Exception e3) {
                                            System.out.println("WARNING: Formato de tiempo inválido: " + timeStr); // LOG
                                            time = null;
                                        }
                                    }
                                }
                            } else if (ctime.getCellType() == CellType.NUMERIC) {
                                time = ctime.getDateCellValue(); // Si es Date cell
                            }
                            // Calcular tiempoGlobal (ejemplo: minutos desde inicio, ajusta lógica)
                            if (time != null) {
                                tiempoGlobal = (time.getTime() / 60000.0); // ms a minutos
                            }
                        } catch (Exception e) {
                            System.out.println("WARNING: Error parsing tiempo en fila " + fila.getRowNum() + ": " + e.getMessage()); // LOG
                            tiempoGlobal = filasProcesadas * 0.1; // Fallback aproximado
                        }
                    }

                    // Celda 6: ID (NUMERIC o STRING)
                    org.apache.poi.ss.usermodel.Cell cid = fila.getCell(6);
                    Integer id = null;
                    if (cid != null) {
                        try {
                            if (cid.getCellType() == CellType.NUMERIC) {
                                id = (int) cid.getNumericCellValue();
                            } else if (cid.getCellType() == CellType.STRING) {
                                id = Integer.parseInt(cid.getStringCellValue().trim());
                            }
                        } catch (NumberFormatException ex) {
                            System.out.println("WARNING: ID inválido en fila " + fila.getRowNum() + ". Usando null."); // LOG
                            id = null;
                        }
                    }

                    // Crear y agregar Accion
                    Accion accion = new Accion(id, eqObj, jugador, accionTexto, time, cuarto, tanteoLocal, tanteoVisitante);
                    accion.setTiempoGlobal(tiempoGlobal); // Asume Accion tiene setTiempoGlobal(double)

                    jugador.agregarAccion(accion); // Al jugador (como antes)
                    eqObj.agregarAccion(accion); // NUEVO: Al equipo (para análisis de quintetos)
                    eqObj.agregarJugador(jugador); // Asegura que el jugador esté en el equipo

                    accionesCreadas++;
                    //System.out.println("DEBUG: Creada acción #" + accionesCreadas + ": '" + accionTexto + "' por " + nombre + " en cuarto " + cuarto + ", tiempo: " + tiempoGlobal); // LOG

                    // Clasificar y sumar estadísticas al jugador (mejorado con contains para flexibilidad)
                    String accionLower = accionTexto.toLowerCase();
                    if (accionLower.contains("tiro de 2") && accionLower.contains("anotado")) {
                        jugador.setTiros2(jugador.getTiros2() + 1);
                        jugador.setTiros2M(jugador.getTiros2M() + 1);
                    } else if (accionLower.contains("tiro de 2") && accionLower.contains("fallado")) {
                        jugador.setTiros2(jugador.getTiros2() + 1);
                    } else if (accionLower.contains("tiro de 3") && accionLower.contains("anotado")) {
                        jugador.setTiros3(jugador.getTiros3() + 1);
                        jugador.setTiros3M(jugador.getTiros3M() + 1);
                    } else if (accionLower.contains("tiro de 3") && accionLower.contains("fallado")) {
                        jugador.setTiros3(jugador.getTiros3() + 1);
                    } else if (accionLower.contains("tiro de 1") && accionLower.contains("anotado")) {
                        jugador.setTirosL(jugador.getTirosL() + 1);
                        jugador.setTirosLM(jugador.getTirosLM() + 1);
                    } else if (accionLower.contains("tiro de 1") && accionLower.contains("fallado")) {
                        jugador.setTirosL(jugador.getTirosL() + 1);
                    } else if (accionLower.contains("rebote")) {
                        jugador.setRebotes(jugador.getRebotes() + 1);
                    }
                    // Agrega más condiciones según las acciones que manejes (e.g., asistencias, pérdidas)

                } catch (Exception e) {
                    System.out.println("ERROR procesando fila " + fila.getRowNum() + ": " + e.getMessage()); // LOG
                    e.printStackTrace();
                    continue; // Skip fila problemática
                }
            }

            System.out.println("DEBUG: Lectura completada. Filas procesadas: " + filasProcesadas + ", Acciones creadas: " + accionesCreadas); // LOG RESUMEN
            for (Equipo e : equipos) {
                System.out.println("DEBUG: Equipo '" + e.getNombre() + "': " + e.getAcciones().length + " acciones totales."); // LOG POR EQUIPO
            }

        } catch (IOException e) {
            System.out.println("ERROR al leer archivo Excel: " + e.getMessage()); // LOG
            e.printStackTrace();
        }

        // Generar PDF con tabla de jugadores (solo si hay datos)
        if (!jugadores.isEmpty()) {
            try (PdfWriter writer = new PdfWriter("../jugadores.pdf");
                 PdfDocument pdfDoc = new PdfDocument(writer);
                 Document document = new Document(pdfDoc)) {

                Table table = new Table(new float[]{3, 1, 1, 1, 1, 1, 1, 1});
                table.setWidth(UnitValue.createPercentValue(100));

                // Encabezados
                table.addHeaderCell(new Cell().add(new Paragraph("Nombre")));
                table.addHeaderCell(new Cell().add(new Paragraph("Tiros2")));
                table.addHeaderCell(new Cell().add(new Paragraph("Tiros2M")));
                table.addHeaderCell(new Cell().add(new Paragraph("Tiros3")));
                table.addHeaderCell(new Cell().add(new Paragraph("Tiros3M")));
                table.addHeaderCell(new Cell().add(new Paragraph("TirosL")));
                table.addHeaderCell(new Cell().add(new Paragraph("TirosLM")));
                table.addHeaderCell(new Cell().add(new Paragraph("Rebotes")));

                // Datos de jugadores
                for (Jug jugador : jugadores) {
                    table.addCell(new Cell().add(new Paragraph(jugador.getNombre())));
                    table.addCell(new Cell().add(new Paragraph(String.valueOf(jugador.getTiros2()))));
                    table.addCell(new Cell().add(new Paragraph(String.valueOf(jugador.getTiros2M()))));
                    table.addCell(new Cell().add(new Paragraph(String.valueOf(jugador.getTiros3()))));
                    table.addCell(new Cell().add(new Paragraph(String.valueOf(jugador.getTiros3M()))));
                    table.addCell(new Cell().add(new Paragraph(String.valueOf(jugador.getTirosL()))));
                    table.addCell(new Cell().add(new Paragraph(String.valueOf(jugador.getTirosLM()))));
                    table.addCell(new Cell().add(new Paragraph(String.valueOf(jugador.getRebotes()))));
                }
                document.add(table);
                System.out.println("DEBUG: PDF de jugadores generado correctamente."); // LOG
            } catch (Exception e) {
                System.out.println("ERROR generando PDF de jugadores: " + e.getMessage()); // LOG
                e.printStackTrace();
            }
        } else {
            System.out.println("WARNING: No hay jugadores para generar PDF."); // LOG
        }

        // Generar PDF con tabla de acciones (solo si hay acciones)
        int totalAcciones = 0;
        for (Jug j : jugadores) {
            totalAcciones += j.getAcciones().size();
        }
        if (totalAcciones > 0) {
            try (PdfWriter writer = new PdfWriter("../acciones.pdf");
                 PdfDocument pdfDoc = new PdfDocument(writer);
                 Document document = new Document(pdfDoc)) {

                Table table = new Table(new float[]{3, 1, 5, 2});
                table.setWidth(UnitValue.createPercentValue(100));

                // Encabezados
                table.addHeaderCell(new Cell().add(new Paragraph("Jugador")));
                table.addHeaderCell(new Cell().add(new Paragraph("Nº Acción")));
                table.addHeaderCell(new Cell().add(new Paragraph("Acción")));
                table.addHeaderCell(new Cell().add(new Paragraph("Tiempo")));

                // Datos de acciones
                for (Jug jugador : jugadores) {
                    for (Accion accion : jugador.getAcciones()) {
                        table.addCell(new Cell().add(new Paragraph(jugador.getNombre())));
                        table.addCell(new Cell().add(new Paragraph(String.valueOf(accion.getId()))));
                        table.addCell(new Cell().add(new Paragraph(accion.getAccion())));
                        table.addCell(new Cell().add(new Paragraph(accion.getTiempoS() != null ? accion.getTiempoS() : "N/A")));
                    }
                }
                document.add(table);
                System.out.println("DEBUG: PDF de acciones generado correctamente."); // LOG
            } catch (Exception e) {
                System.out.println("ERROR generando PDF de acciones: " + e.getMessage()); // LOG
                e.printStackTrace();
            }
        } else {
            System.out.println("WARNING: No hay acciones para generar PDF."); // LOG
        }
    }

    public Equipo getEquipo(String nombreEquipo) {
        if (nombreEquipo == null) return null;
        nombreEquipo = nombreEquipo.trim(); // eliminar espacios al inicio/final

        for (Equipo e : equipos) {
            if (e.getNombre() != null && e.getNombre().trim().equalsIgnoreCase(nombreEquipo)) {
                return e;
            }
        }
        return null; // no se encontró
    }
}
