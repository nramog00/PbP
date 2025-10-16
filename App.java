import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import stats.AnalizadorQuintetos;
import stats.Equipo;
import stats.Jug;
//import stats.PruebaQuintetos;
import stats.Quinteto;

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

        // Leer todas las líneas
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(INPUT_TXT_PATH))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line.trim());
            }
        }

        // Invertir lista para procesar cronológicamente
        Collections.reverse(lines);

        // Variables de control
        int currentQuarter = 1;
        int scoreLocal = 0;
        int scoreVisitante = 0;
        String lastTime = "";
        boolean inicio = false;
        int ultimoCuarto = 0;

        for (String line : lines) {
            // Detectar inicio de cuarto
            if (line.contains("Comienzo del Cuarto 1")) {
                currentQuarter = 1;
                inicio = true;
            } else if (line.contains("Comienzo del Cuarto 2")) {
                currentQuarter = 2;
                inicio = true;
            } else if (line.contains("Comienzo del Cuarto 3")) {
                currentQuarter = 3;
                inicio = true;
            } else if (line.contains("Comienzo del Cuarto 4")) {
                currentQuarter = 4;
                inicio = true;
            } else if (line.contains("Comienzo de la Prórroga")) {
                currentQuarter = 5;
                inicio = true;
            }

            if (inicio && currentQuarter != ultimoCuarto) { // Esperar al primer cuarto
                // Insertar acción “inicio de cuarto”
                lastTime = "10:00";
                rows.add(new String[]{
                        "MIPELLETYMAS B.F. LEON",
                        "CUARTO " + currentQuarter,
                        "Inicio del cuarto",
                        lastTime,
                        String.valueOf(currentQuarter),
                        toString((currentQuarter - 1) * DURACION_CUARTO),
                        String.valueOf(scoreLocal),
                        String.valueOf(scoreVisitante)
                });
                rows.add(new String[]{
                        "ROBLES LLEIDA", // cambia esto por el nombre exacto del rival
                        "CUARTO " + currentQuarter,
                        "Inicio del cuarto",
                        lastTime,
                        String.valueOf(currentQuarter),
                        toString((currentQuarter - 1) * DURACION_CUARTO),
                        String.valueOf(scoreLocal),
                        String.valueOf(scoreVisitante)
                });
                inicio = false;
                ultimoCuarto = currentQuarter;
            }

            // Detectar marcador
            if (line.matches("\\d+\\s*-\\s*\\d+")) {
                String[] scores = line.split("-");
                scoreLocal = Integer.parseInt(scores[0].trim());
                scoreVisitante = Integer.parseInt(scores[1].trim());
            }

            // Detectar línea de tiempo
            else if (line.matches("\\d{2}:\\d{2}.*")) {
                lastTime = line.split(" ")[0];
            }

            // Detectar acción
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
                            toString(tiempoGlobal),
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
            double tiempoMin = Double.parseDouble(r[5]);       // minutos acumulados
            double excelTime = tiempoMin / (24 * 60.0);       // convertir a fracción de día
            Cell cell = row.createCell(5);
            cell.setCellValue(excelTime);
            cell.setCellStyle(timeStyle);                      // formato mm:ss
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

    

        // Leer Excel y generar estructuras
        lector.leerArchivoExcel(OUTPUT_XLSX_PATH);
        System.out.println("Archivo Excel leído y procesado correctamente.");

        // === NUEVO: Analizar quintetos y añadir hoja ===
        Equipo miEquipo = lector.getEquipo("MIPELLETYMAS B.F. LEON");
        Equipo rival = lector.getEquipo("ROBLES LLEIDA"); // Cambiar por el nombre real

        if (miEquipo == null || rival == null) {
            System.out.println("No se encontró el equipo especificado.");
            return;
        }
        AnalizadorQuintetos analizador = new AnalizadorQuintetos();
        Map<String, Quinteto> stats = analizador.analizarQuintetos(miEquipo);
        Map<String, Quinteto> statsRival = analizador.analizarQuintetos(rival);

        try (FileInputStream fis = new FileInputStream(OUTPUT_XLSX_PATH);
             Workbook workbook2 = new XSSFWorkbook(fis)) {

            Sheet sheetQ = workbook2.createSheet("Quintetos");

            // Encabezado
            Row headerQ = sheetQ.createRow(0);
            String[] columnas = {
                    "Cuarto", "Jugadores", "Minutos", "Puntos", "T2M", "T2A", "T3M", "T3A",
                    "TLM", "TLA", "RebOf", "RebDef", "Perdidas"
            };
            for (int i = 0; i < columnas.length; i++) {
                headerQ.createCell(i).setCellValue(columnas[i]);
            }

            CreationHelper createHelper2 = workbook2.getCreationHelper();
            CellStyle timeStyle2 = workbook2.createCellStyle();
            timeStyle2.setDataFormat(createHelper2.createDataFormat().getFormat("mm:ss"));
            sheetQ.setDefaultColumnStyle(2, timeStyle2);

            int rowNum = 1;
            for (Quinteto q : stats.values()) {
                Row row = sheetQ.createRow(rowNum++);
                int c = 0;
                row.createCell(c++).setCellValue(q.getCuarto());
                row.createCell(c++).setCellValue(
                    q.getJugadores().stream()
                    .map(Jug::getNombre)
                    .sorted() // opcional, para orden alfabético
                    .collect(java.util.stream.Collectors.joining(", "))
                );
                double tiempoMin2 = q.getMinutosJugados();       // minutos acumulados
                double excelTime2 = tiempoMin2 / (24 * 60.0);       // convertir a fracción de día
                Cell cell2 = row.createCell(c++);
                cell2.setCellValue(excelTime2);
                cell2.setCellStyle(timeStyle2);
                row.createCell(c++).setCellValue(q.getPuntos());
                row.createCell(c++).setCellValue(q.getT2met());
                row.createCell(c++).setCellValue(q.getT2int());
                row.createCell(c++).setCellValue(q.getT3met());
                row.createCell(c++).setCellValue(q.getT3int());
                row.createCell(c++).setCellValue(q.getTlmet());
                row.createCell(c++).setCellValue(q.getTlint());
                row.createCell(c++).setCellValue(q.getRebOf());
                row.createCell(c++).setCellValue(q.getRebDef());
                row.createCell(c++).setCellValue(q.getPerdidas());

                Quinteto rivalQ = buscarRival(q, statsRival);
                if (rivalQ != null) {
                    Row rowRival = sheetQ.createRow(rowNum++);
                    int r = 1;
                    rowRival.createCell(r++).setCellValue("→ Rival (" + rival.getNombre() + ")");
                    double tiempoRival = rivalQ.getMinutosJugados();
                    double excelTimeRival = tiempoRival / (24 * 60.0);
                    Cell rivalTimeCell = rowRival.createCell(r++);
                    rivalTimeCell.setCellValue(excelTimeRival);
                    rivalTimeCell.setCellStyle(timeStyle2);
                    rowRival.createCell(r++).setCellValue(rivalQ.getPuntos());
                    rowRival.createCell(r++).setCellValue(rivalQ.getT2met());
                    rowRival.createCell(r++).setCellValue(rivalQ.getT2int());
                    rowRival.createCell(r++).setCellValue(rivalQ.getT3met());
                    rowRival.createCell(r++).setCellValue(rivalQ.getT3int());
                    rowRival.createCell(r++).setCellValue(rivalQ.getTlmet());
                    rowRival.createCell(r++).setCellValue(rivalQ.getTlint());
                    rowRival.createCell(r++).setCellValue(rivalQ.getRebOf());
                    rowRival.createCell(r++).setCellValue(rivalQ.getRebDef());
                    rowRival.createCell(r++).setCellValue(rivalQ.getPerdidas());
                }
            }
            try (FileOutputStream fos = new FileOutputStream(OUTPUT_XLSX_PATH)) {
                workbook2.write(fos);
            }
        }

        System.out.println("Hoja 'Quintetos' añadida correctamente al archivo Excel.");

        //PruebaQuintetos.imprimirQuintetos(stats);
    }

    /**
     * Convierte un tiempo (mm:ss) en minutos globales acumulados desde el inicio del partido.
     * Ejemplo: Cuarto 2, tiempo "09:15" → (2-1)*10 + (10 - 9.25) = 10.75
     */
    private static double convertirTiempoGlobal(String tiempoRestante, int cuarto, double duracionCuarto) {
        try {
            String[] partes = tiempoRestante.split(":");
            int min = Integer.parseInt(partes[0]);
            int sec = Integer.parseInt(partes[1]);
            double minDec = min + sec / 60.0;

            // Tiempo transcurrido dentro del cuarto
            double tiempoTranscurridoCuarto = duracionCuarto - minDec;

            // Tiempo global desde el inicio del partido
            return (cuarto - 1) * duracionCuarto + tiempoTranscurridoCuarto;
        } catch (Exception e) {
            return (cuarto - 1) * duracionCuarto;
        }
    }

    private static String toString(double time) {
        return String.format(Locale.US, "%.2f", time);
    }

    private static Quinteto buscarRival(Quinteto q, Map<String, Quinteto> rivales) {
        for (Quinteto r : rivales.values()) {
            if (r.getCuarto() == q.getCuarto()) {
                double inicioQ = q.getTiempoInicio();
                double finQ = q.getTiempoFin();
                double inicioR = r.getTiempoInicio();
                double finR = r.getTiempoFin();

                // Si coinciden en tiempo o se solapan, consideramos que jugaban a la vez
                if (inicioR < finQ && finR > inicioQ) {
                    return r;
                }
            }
        }
        return null;
    }

}
