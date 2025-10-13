package stats;

import java.util.ArrayList;
import java.util.List;

public class Equipo {
    
    private Integer id;
    private String nombre;
    private String abreviatura;
    private Integer puntos;
    private Integer rebotes;
    private Integer oRebotes;
    private Integer tiros2;
    private Integer tiros2M;
    private Integer tiros3;
    private Integer tiros3M;
    private Integer tirosL;
    private Integer tirosLM;

    private List<Accion> acciones = new ArrayList<>();

    public Equipo() {
    }

    public Equipo(Integer id, String nombre, String abreviatura) {
        this.id = id;
        this.nombre = nombre;
        this.abreviatura = abreviatura;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getAbreviatura() {
        return abreviatura;
    }

    public void setAbreviatura(String abreviatura) {
        this.abreviatura = abreviatura;
    }
    public Integer getPuntos() {
        return puntos;
    }
    public void setPuntos(Integer puntos) {
        this.puntos = puntos;
    }
    public Integer getRebotes() {
        return rebotes;
    }
    public void setRebotes(Integer rebotes) {
        this.rebotes = rebotes;
    }
    public Integer getoRebotes() {
        return oRebotes;
    }
    public void setoRebotes(Integer oRebotes) {
        this.oRebotes = oRebotes;
    }
    public Integer getTiros2() {
        return tiros2;
    }
    public void setTiros2(Integer tiros2) {
        this.tiros2 = tiros2;
    }
    public Integer getTiros2M() {
        return tiros2M;
    }
    public void setTiros2M(Integer tiros2M) {
        this.tiros2M = tiros2M;
    }
    public Integer getTiros3() {
        return tiros3;
    }
    public void setTiros3(Integer tiros3) {
        this.tiros3 = tiros3;
    }
    public Integer getTiros3M() {
        return tiros3M;
    }
    public void setTiros3M(Integer tiros3M) {
        this.tiros3M = tiros3M;
    }
    public Integer getTirosL() {
        return tirosL;
    }
    public void setTirosL(Integer tirosL) {
        this.tirosL = tirosL;
    }
    public Integer getTirosLM() {
        return tirosLM;
    }
    public void setTirosLM(Integer tirosLM) {
        this.tirosLM = tirosLM;
    }

    public void agregarAccion(Accion accion) {
        if (accion != null) {
            acciones.add(accion);
        }
    }

    public Accion[] getAcciones() {
        return acciones.toArray(new Accion[0]); // Devuelve un array de Accion
    }

    public List<Accion> getAccionesList() {
        return acciones; // Opcional: devuelve la lista directamente
    }
}
