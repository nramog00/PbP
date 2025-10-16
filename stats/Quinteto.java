package stats;

import java.util.*;


public class Quinteto {
    private int cuarto;
    private Set<Jug> jugadores = new HashSet<>();

    // Estadísticas
    private int puntos = 0;
    private int t2met = 0, t2int = 0;
    private int t3met = 0, t3int = 0;
    private int tlmet = 0, tlint = 0;
    private int rebOf = 0, rebDef = 0;
    private int perdidas = 0;

    private double tiempoInicio = 0.0;
    private double tiempoFin = 0.0;

    public Quinteto(int cuarto, Set<Jug> jugadores) {
        this.cuarto = cuarto;
        this.jugadores.addAll(jugadores);
    }

    // Getters y setters
    public int getCuarto() { return cuarto; }
    public void setCuarto(int cuarto) { this.cuarto = cuarto; }
    public Set<Jug> getJugadores() { return jugadores; }
    public double getTiempoInicio() { return tiempoInicio; }
    public void setTiempoInicio(double t) { tiempoInicio = t; }
    public double getTiempoFin() { return tiempoFin; }
    public void setTiempoFin(double t) { tiempoFin = t; }
    public int getPuntos() { return puntos; }
    public int getT2met() { return t2met; }
    public int getT2int() { return t2int; }
    public int getT3met() { return t3met; }
    public int getT3int() { return t3int; }
    public int getTlmet() { return tlmet; }
    public int getTlint() { return tlint; }
    public int getRebOf() { return rebOf; }
    public int getRebDef() { return rebDef; }
    public int getPerdidas() { return perdidas; }

    // Método para sumar estadísticas
    public void agregarAccion(Accion a) {
        String accion = a.getAccion().toLowerCase();
        if (accion.contains("tiro de 2")) { t2int++; if (accion.contains("anotado")) { t2met++; puntos += 2; } }
        else if (accion.contains("tiro de 3")) { t3int++; if (accion.contains("anotado")) { t3met++; puntos += 3; } }
        else if (accion.contains("tiro libre")) { tlint++; if (accion.contains("anotado")) { tlmet++; puntos += 1; } }
        else if (accion.contains("rebote ofensivo")) rebOf++;
        else if (accion.contains("rebote defensivo")) rebDef++;
        else if (accion.contains("pérdida") || accion.contains("perdida")) perdidas++;
        else if (accion.contains("comienzo del cuarto")) this.cuarto = a.getCuarto(); // Actualizar cuarto si es necesario{
            // No sumar puntos, pero podría llevar un contador de faltas si se desea
        tiempoFin = a.getTiempoGlobal();
    }

    public double getMinutosJugados() { return tiempoFin - tiempoInicio; }

    // Opcional: clave única basada en jugadores (ordenados) y cuarto
    public String generarClave() {
        List<String> lista = new ArrayList<>();
        for (Jug j : jugadores) lista.add(j.getNombre());
        Collections.sort(lista);
        return cuarto + " - " + String.join(", ", lista);
    }
}
