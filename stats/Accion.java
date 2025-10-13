package stats;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Accion {
    private Integer id;
    private Equipo equipo;
    private Jug jugador;
    private String accion;
    private Date tiempo;
    private int tanteoLocal;
    private int tanteoVisitante;

    // 🆕 Nuevos campos
    private int cuarto;         // número de cuarto (1, 2, 3, 4)
    private double tiempoGlobal; // minutos acumulados desde el inicio del partido (ej: 13.75)
    private String segmento;     // "Q1" o "Resto"

    // =====================
    // Constructores
    // =====================
    public Accion() {}

    public Accion(Integer id, Equipo equipo, Jug jugador, String accion, Date tiempo, int cuarto, int tanteoLocal, int tanteoVisitante) {
        this.id = id;
        this.equipo = equipo;
        this.jugador = jugador;
        this.accion = accion;
        this.tiempo = tiempo;
        this.cuarto = cuarto;
        this.tanteoLocal = tanteoLocal;
        this.tanteoVisitante = tanteoVisitante;
        calcularTiempoGlobal();
    }

    // =====================
    // Getters y setters
    // =====================

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Equipo getEquipo() { return equipo; }
    public void setEquipo(Equipo equipo) { this.equipo = equipo; }

    public Jug getJugador() { return jugador; }
    public void setJugador(Jug jugador) { this.jugador = jugador; }

    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion = accion; }

    public Date getTiempo() { return tiempo; }
    public void setTiempo(Date tiempo) { 
        this.tiempo = tiempo;
        calcularTiempoGlobal(); // recalcular si cambia
    }

    public int getCuarto() { return cuarto; }
    public void setCuarto(int cuarto) { 
        this.cuarto = cuarto; 
        calcularTiempoGlobal();
    }

    public double getTiempoGlobal() { return tiempoGlobal; }
    public String getSegmento() { return segmento; }

    public int getTanteoLocal() { return tanteoLocal; }
    public int getTanteoVisitante() { return tanteoVisitante; }
    public void setTanteoLocal(int tanteoLocal) { this.tanteoLocal = tanteoLocal; }
    public void setTanteoVisitante(int tanteoVisitante) { this.tanteoVisitante = tanteoVisitante; }

    // =====================
    // Métodos auxiliares
    // =====================

    /**
     * Calcula el tiempo global del partido en minutos decimales.
     * Asume cuartos de 10 minutos.
     */
    private void calcularTiempoGlobal() {
        if (tiempo == null) {
            this.tiempoGlobal = 0;
            this.segmento = "Q1";
            return;
        }
        SimpleDateFormat sdfMin = new SimpleDateFormat("mm");
        SimpleDateFormat sdfSec = new SimpleDateFormat("ss");
        try {
            int minutos = Integer.parseInt(sdfMin.format(tiempo));
            int segundos = Integer.parseInt(sdfSec.format(tiempo));
            double totalMin = minutos + segundos / 60.0;
            this.tiempoGlobal = (cuarto - 1) * 10.0 + totalMin;
            this.segmento = (cuarto == 1) ? "Q1" : "Resto";
        } catch (NumberFormatException e) {
            this.tiempoGlobal = 0;
            this.segmento = "Q1";
        }
    }

    /**
     * Devuelve el tiempo formateado mm:ss
     */
    public String getTiempoS() {
        if (tiempo != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("mm:ss");
            return sdf.format(tiempo);
        } else {
            return "";
        }
    }

    @Override
    public String toString() {
        return "Accion{" +
                "id=" + id +
                ", equipo=" + (equipo != null ? equipo.getNombre() : "null") +
                ", jugador=" + (jugador != null ? jugador.getNombre() : "null") +
                ", accion='" + accion + '\'' +
                ", cuarto=" + cuarto +
                ", tiempo=" + getTiempoS() +
                ", tiempoGlobal=" + String.format("%.2f", tiempoGlobal) +
                ", segmento=" + segmento +
                ", tanteoLocal=" + tanteoLocal +
                ", tanteoVisitante=" + tanteoVisitante +
                '}';
    }
}
