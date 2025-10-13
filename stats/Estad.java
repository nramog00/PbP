package stats;

public class Estad {
    public void EstadJug(){
        Jug jug = new Jug();
        jug.setTiempo(calcTiempo(jug.getId().toString()));
    }

    // Calcula el tiempo total en pista y lo devuelve como un java.util.Date (para guardar en Excel)
    // Supongamos que tienes una clase ExcelReader con métodos para obtener los tiempos de entrada/salida
    private java.util.Date calcTiempo(String jugadorId) {
        List<java.util.Date> entradas = ExcelReader.getTiemposEntrada(jugadorId);
        List<java.util.Date> salidas = ExcelReader.getTiemposSalida(jugadorId);

        if (entradas == null || salidas == null) {
            return null;
        }
        long tiempoTotal = 0;
        int size = Math.min(entradas.size(), salidas.size());
        for (int i = 0; i < size; i++) {
            java.util.Date entrada = entradas.get(i);
            java.util.Date salida = salidas.get(i);
            if (entrada != null && salida != null) {
                tiempoTotal += salida.getTime() - entrada.getTime();
            }
        }
        return tiempoTotal > 0 ? new java.util.Date(tiempoTotal) : null;
    }
}
