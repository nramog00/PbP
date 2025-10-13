import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import stats.Accion;
import stats.Equipo;
import stats.Jug;

public class LeerExcel {
    List<Equipo> equipos = new java.util.ArrayList<>();
    List<Jug> jugadores = new java.util.ArrayList<>();

    public void leerArchivoExcel(String rutaArchivo) {
        int eqId = 0;
        try (FileInputStream fis = new FileInputStream(new File(rutaArchivo));
            Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet hoja = workbook.getSheetAt(0);
            for (Row fila : hoja) {
                String nombre = fila.getCell(1).getStringCellValue();
                Jug jugador = null;

                // Buscar si el jugador ya existe en la lista
                for (Jug j : jugadores) {
                    if (j.getNombre().equalsIgnoreCase(nombre)) {
                        jugador = j;
                        break;
                    }
                }
                // Si no existe, crear y añadir
                if (jugador == null) {
                    jugador = new Jug(nombre);
                    jugadores.add(jugador);
                }

                // Leer el nombre del equipo y normalizarlo
                Cell cellEquipo = fila.getCell(0);
                String equipo = (cellEquipo != null) ? cellEquipo.getStringCellValue().trim() : "DESCONOCIDO";

                // Buscar si el equipo ya existe en la lista global de equipos
                Equipo eqObj = null;
                for (Equipo e : this.equipos) { // 'equipos' debe ser atributo de la clase
                    if (e.getNombre() != null && e.getNombre().trim().equalsIgnoreCase(equipo)) {
                        eqObj = e;
                        break;
                    }
                }

                // Si no existe, crear y añadir
                if (eqObj == null) {
                    eqObj = new Equipo(eqId++, equipo, equipo); // id autoincremental
                    this.equipos.add(eqObj); // añadir a la lista global de equipos
                }

                // Asignar el equipo al jugador
                jugador.setEquipo(eqObj);

                int cuarto = 1;
                Cell cellCuarto = fila.getCell(4);
                if (cellCuarto != null && cellCuarto.getCellType() == CellType.NUMERIC) {
                    cuarto = (int) cellCuarto.getNumericCellValue();
                }

                int tanteoLocal = 0;
                Cell cellTanteoLocal = fila.getCell(7);
                if (cellTanteoLocal != null && cellTanteoLocal.getCellType() == CellType.NUMERIC) {
                    tanteoLocal = (int) cellTanteoLocal.getNumericCellValue();
                }

                int tanteoVisitante = 0;
                Cell cellTanteoVisitante = fila.getCell(8);
                if (cellTanteoVisitante != null && cellTanteoVisitante.getCellType() == CellType.NUMERIC) {
                    tanteoVisitante = (int) cellTanteoVisitante.getNumericCellValue();
                }

                // Leer solo la celda de la acción (índice 2)
                Cell acc = fila.getCell(2);
                Cell ctime = fila.getCell(3);
                java.util.Date time = null;
                try {
                    String timeStr = ctime.getStringCellValue();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // Adjust format to your data
                    time = sdf.parse(timeStr);
                } catch (Exception e) {
                    time = null;
                }
                Cell cid = fila.getCell(6);
                Integer id = null;
                if (cid != null && cid.getCellType() == CellType.NUMERIC) {
                    id = (int) cid.getNumericCellValue();
                } else if (cid != null && cid.getCellType() == CellType.STRING) {
                    try {
                        id = Integer.parseInt(cid.getStringCellValue());
                    } catch (NumberFormatException ex) {
                        id = null;
                    }
                }
                if (acc != null) {
                    String accionTexto;
                    if (acc.getCellType() == CellType.STRING) {
                        accionTexto = acc.getStringCellValue();
                    } else if (acc.getCellType() == CellType.NUMERIC) {
                        accionTexto = String.valueOf(acc.getNumericCellValue());
                    } else {
                        accionTexto = "";
                    }
                    Accion accion = new Accion(id, eqObj, jugador, accionTexto, time, cuarto, tanteoLocal, tanteoVisitante);

                    jugador.getAcciones().add(accion);

                    // Clasificar y sumar estadísticas
                    if (accionTexto.equalsIgnoreCase("Tiro de 2 anotado")) {
                        jugador.setTiros2(jugador.getTiros2() + 1);
                        jugador.setTiros2M(jugador.getTiros2M() + 1);
                    } else if (accionTexto.equalsIgnoreCase("Tiro de 2 fallado")) {
                        jugador.setTiros2(jugador.getTiros2() + 1);
                    } else if (accionTexto.equalsIgnoreCase("Tiro de 3 anotado")) {
                        jugador.setTiros3(jugador.getTiros3() + 1);
                        jugador.setTiros3M(jugador.getTiros3M() + 1);
                    } else if (accionTexto.equalsIgnoreCase("Tiro de 3 fallado")) {
                        jugador.setTiros3(jugador.getTiros3() + 1);
                    } else if (accionTexto.equalsIgnoreCase("Tiro de 1 anotado")) {
                        jugador.setTirosL(jugador.getTirosL() + 1);
                        jugador.setTirosLM(jugador.getTirosLM() + 1);
                    } else if (accionTexto.equalsIgnoreCase("Tiro de 1 fallado")) {
                        jugador.setTirosL(jugador.getTirosL() + 1);
                    } else if (accionTexto.equalsIgnoreCase("Rebote")) {
                        jugador.setRebotes(jugador.getRebotes() + 1);
                    }
                    // Agrega más condiciones según las acciones que manejes
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Generar PDF con tabla de jugadores
        try (com.itextpdf.kernel.pdf.PdfWriter writer = new com.itextpdf.kernel.pdf.PdfWriter("../jugadores.pdf");
             com.itextpdf.kernel.pdf.PdfDocument pdfDoc = new com.itextpdf.kernel.pdf.PdfDocument(writer);
             com.itextpdf.layout.Document document = new com.itextpdf.layout.Document(pdfDoc)) {

            com.itextpdf.layout.element.Table table = new com.itextpdf.layout.element.Table(new float[]{3, 1, 1, 1, 1, 1, 1, 1});
            table.setWidth(com.itextpdf.layout.properties.UnitValue.createPercentValue(100));

            // Encabezados
            table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph("Nombre")));
            table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph("Tiros2")));
            table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph("Tiros2M")));
            table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph("Tiros3")));
            table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph("Tiros3M")));
            table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph("TirosL")));
            table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph("TirosLM")));
            table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph("Rebotes")));

            // Datos de jugadores
            for (Jug jugador : jugadores) {
            table.addCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph(jugador.getNombre())));
            table.addCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph(String.valueOf(jugador.getTiros2()))));
            table.addCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph(String.valueOf(jugador.getTiros2M()))));
            table.addCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph(String.valueOf(jugador.getTiros3()))));
            table.addCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph(String.valueOf(jugador.getTiros3M()))));
            table.addCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph(String.valueOf(jugador.getTirosL()))));
            table.addCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph(String.valueOf(jugador.getTirosLM()))));
            table.addCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph(String.valueOf(jugador.getRebotes()))));
            }
            document.add(table);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Generar PDF con tabla de acciones
        try (com.itextpdf.kernel.pdf.PdfWriter writer = new com.itextpdf.kernel.pdf.PdfWriter("../acciones.pdf");
             com.itextpdf.kernel.pdf.PdfDocument pdfDoc = new com.itextpdf.kernel.pdf.PdfDocument(writer);
             com.itextpdf.layout.Document document = new com.itextpdf.layout.Document(pdfDoc)) {

            com.itextpdf.layout.element.Table table = new com.itextpdf.layout.element.Table(new float[]{3, 1, 5, 2});
            table.setWidth(com.itextpdf.layout.properties.UnitValue.createPercentValue(100));

            // Encabezados
            table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph("Jugador")));
            table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph("Nº Acción")));
            table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph("Acción")));
            table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph("Tiempo")));

            // Datos de acciones
            for (Jug jugador : jugadores) {
                for (Accion accion : jugador.getAcciones()) {
                    table.addCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph(jugador.getNombre())));
                    table.addCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph(String.valueOf(accion.getId()))));
                    table.addCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph(accion.getAccion())));
                    table.addCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph(accion.getTiempoS())));
                }
            }
            document.add(table);
        } catch (Exception e) {
            e.printStackTrace();
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
