import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import stats.AnalizadorQuintetos;
import stats.Equipo;

import java.io.*;
import java.util.*;
import java.util.regex.*;

public class App {
    private static final String INPUT_TXT_PATH = "C:/Users/Usuario/Documents/PbP/bfl.txt";
    private static final String OUTPUT_XLSX_PATH = "C:/Users/Usuario/Documents/PbP/bfl.xlsx";
    private static final double DURACION_CUARTO = 10.0;

    public static void main(String[] args) throws Exception {
        LeerExcel lector = new LeerExcel();
        List<String[]> rows = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\(([^)]+)\\)\\s*([^:]+):?\\s*(.*)");

        try (BufferedReader br = new BufferedReader(new FileReader(INPUT_TXT_PATH))) {
            String line;
            String lastTime = "";
            int scoreLocal = 0;
            int scoreVisitante = 0;
            int currentQuarter = 1;

            while ((line = br.readLine()) != null) {
                line = line.trim();

                // Detectar inicio de cuarto
                if (line.contains("Comienzo del Cuarto 1")) currentQuarter = 1;
                else if (line.contains("Comienzo del Cuarto 2")) currentQuarter = 2;
                else if (line.contains("Comienzo del Cuarto 3")) currentQuarter = 3;
                else if (line.contains("Comienzo del Cuarto 4")) currentQuarter = 4;
                else if (line.contains("Comienzo de la Prórroga")) currentQuarter = 5;

                // Detectar marcador
                if (line.matches("\\d+\\s*-\\s*\\d+")) {
                    String[] scores = line.split("-");
                    scoreLocal = Integer.parseInt(scores[0].trim());
                    scoreVisitante = Integer.parseInt(scores[1].trim());
                    continue;
                }

                // Detectar línea de tiempo (ejemplo "09:43 ••••")
                else if (line.matches("\\d{2}:\\d{2}.*")) {
                    lastTime = line.split(" ")[0];
                }
                // Detectar acción (línea que empieza por "(")
                else if (line.startsWith("(")) {
                    Matcher m = pattern.matcher(line);
                    if (m.find()) {
                        String equipo = m.group(1).trim();
                        String jugador = m.group(2).trim();
                        String accion = m.group(3).trim();

                        double tiempoGlobal = convertirTiempoGlobal(lastTime, currentQuarter, DURACION_CUARTO);

                        rows.add(new String[]{
                                equipo,
                                jugador,
                                accion,
                                lastTime,
                                String.valueOf(currentQuarter),
                                String.valueOf(tiempoGlobal),
                                String.valueOf(scoreLocal),
                                String.valueOf(scoreVisitante)
                        });
                    }
                }
            }

            // ===== Crear Excel =====
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("PlayByPlay");

            // Encabezados
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Equipo");
            header.createCell(1).setCellValue("Jugador");
            header.createCell(2).setCellValue("Accion");
            header.createCell(3).setCellValue("Tiempo");
            header.createCell(4).setCellValue("Cuarto");
            header.createCell(5).setCellValue("TiempoGlobal");
            header.createCell(6).setCellValue("ID");
            header.createCell(7).setCellValue("TanteoLocal");
            header.createCell(8).setCellValue("TanteoVisitante");

            // Formato mm:ss para columnas 3 y 5
            CreationHelper createHelper = workbook.getCreationHelper();
            CellStyle timeStyle = workbook.createCellStyle();
            timeStyle.setDataFormat(createHelper.createDataFormat().getFormat("mm:ss"));
            sheet.setDefaultColumnStyle(3, timeStyle);
            sheet.setDefaultColumnStyle(5, timeStyle);

            // Escribir filas
            for (int i = 0; i < rows.size(); i++) {
                Row row = sheet.createRow(i + 1);
                String[] r = rows.get(i);

                row.createCell(0).setCellValue(r[0]);
                row.createCell(1).setCellValue(r[1]);
                row.createCell(2).setCellValue(r[2]);
                row.createCell(3).setCellValue(r[3]);
                row.createCell(4).setCellValue(Integer.parseInt(r[4]));
                row.createCell(5).setCellValue(Double.parseDouble(r[5]));
                row.createCell(6).setCellValue(i);
                row.createCell(7).setCellValue(Integer.parseInt(r[6]));
                row.createCell(8).setCellValue(Integer.parseInt(r[7]));
            }

            // Guardar primera versión del Excel
            try (FileOutputStream fos = new FileOutputStream(OUTPUT_XLSX_PATH)) {
                workbook.write(fos);
            }
            workbook.close();
            System.out.println("Hoja PlayByPlay creada correctamente.");

        }

        // Leer Excel y generar estructuras
        lector.leerArchivoExcel(OUTPUT_XLSX_PATH);
        System.out.println("Archivo Excel leído y procesado correctamente.");

        // === NUEVO: Analizar quintetos y añadir hoja ===
        /*Equipo miEquipo = lector.getEquipo("MIPELLETYMAS B.F. LEON"); // cambia el nombre según corresponda
        AnalizadorQuintetos analizador = new AnalizadorQuintetos();
        Map<String, AnalizadorQuintetos.QuintetoStats> stats = analizador.analizarQuintetos(miEquipo);*/
        Equipo miEquipo = lector.getEquipo("MIPELLETYMAS B.F. LEON");
        if (miEquipo == null) {
            System.out.println("No se encontró el equipo especificado.");
            return;
        }
        AnalizadorQuintetos analizador = new AnalizadorQuintetos();
        Map<String, AnalizadorQuintetos.QuintetoStats> stats = analizador.analizarQuintetos(miEquipo);

        try (FileInputStream fis = new FileInputStream(OUTPUT_XLSX_PATH);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheetQ = workbook.createSheet("Quintetos");

            // Encabezado
            Row headerQ = sheetQ.createRow(0);
            String[] columnas = {
                    "Cuarto", "Jugadores", "Minutos", "Puntos", "T2M", "T2A", "T3M", "T3A",
                    "TLM", "TLA", "RebOf", "RebDef", "Perdidas"
            };
            for (int i = 0; i < columnas.length; i++) {
                headerQ.createCell(i).setCellValue(columnas[i]);
            }

            int rowNum = 1;
            for (AnalizadorQuintetos.QuintetoStats q : stats.values()) {
                Row row = sheetQ.createRow(rowNum++);
                int c = 0;
                row.createCell(c++).setCellValue(q.cuarto);
                row.createCell(c++).setCellValue(String.join(", ", q.jugadores));
                row.createCell(c++).setCellValue(q.getMinutosJugados());
                row.createCell(c++).setCellValue(q.puntos);
                row.createCell(c++).setCellValue(q.t2met);
                row.createCell(c++).setCellValue(q.t2int);
                row.createCell(c++).setCellValue(q.t3met);
                row.createCell(c++).setCellValue(q.t3int);
                row.createCell(c++).setCellValue(q.tlmet);
                row.createCell(c++).setCellValue(q.tlint);
                row.createCell(c++).setCellValue(q.rebOf);
                row.createCell(c++).setCellValue(q.rebDef);
                row.createCell(c++).setCellValue(q.perdidas);
            }

            try (FileOutputStream fos = new FileOutputStream(OUTPUT_XLSX_PATH)) {
                workbook.write(fos);
            }
        }

        System.out.println("Hoja 'Quintetos' añadida correctamente al archivo Excel.");
    }

    /**
     * Convierte un tiempo (mm:ss) en minutos globales acumulados desde el inicio del partido.
     * Ejemplo: Cuarto 2, tiempo "09:15" → (2-1)*10 + (10 - 9.25) = 10.75
     */
    private static double convertirTiempoGlobal(String tiempo, int cuarto, double duracionCuarto) {
        try {
            String[] partes = tiempo.split(":");
            int min = Integer.parseInt(partes[0]);
            int sec = Integer.parseInt(partes[1]);
            double minDec = min + sec / 60.0;
            double tiempoDentroCuarto = duracionCuarto - minDec;
            return (cuarto - 1) * duracionCuarto + tiempoDentroCuarto;
        } catch (Exception e) {
            return (cuarto - 1) * duracionCuarto;
        }
    }
}
