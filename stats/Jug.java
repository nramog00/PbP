package stats;

import java.util.ArrayList;
import java.util.List;

public class Jug implements java.io.Serializable{
    
    int i=0;
    private List<Accion> acciones = new ArrayList<>();
    
    private Integer id;
    private Equipo equipo;
    private String nombre;
    private Integer dorsal;
    private java.util.Date tiempo;
    private Integer puntos = 0;
    private Integer rebotes = 0;
    private Integer oRebotes = 0;
    private Integer tiros2 = 0;
    private Integer tiros2M = 0;
    private Integer tiros3 = 0;
    private Integer tiros3M = 0;
    private Integer tirosL = 0;
    private Integer tirosLM = 0;

    public Jug() {
        this.acciones = new ArrayList<>();
    }

    public Jug(String nombre) {
        this.nombre = nombre;
        this.acciones = new ArrayList<>();
        this.id = i+1;
        i++;
    }

    public Jug(Integer id, Equipo equipo, String nombre, Integer dorsal, java.util.Date tiempo, Integer puntos, Integer rebotes, Integer oRebotes, Integer tiros2, Integer tiros2M, Integer tiros3, Integer tiros3M, Integer tirosL, Integer tirosLM) {
        this.id = i;
        this.equipo = equipo;
        this.nombre = nombre;
        this.dorsal = dorsal;
        this.tiempo = tiempo;
        this.puntos = puntos;
        this.rebotes = rebotes;
        this.oRebotes = oRebotes;
        this.tiros2 = tiros2;
        this.tiros2M = tiros2M;
        this.tiros3 = tiros3;
        this.tiros3M = tiros3M;
        this.tirosL = tirosL;
        this.tirosLM = tirosLM;
        i++;
    }

    public Integer getId() {
        return id;
    }

    public Integer getId(String nombre) {
        if(nombre == this.nombre){
            return id;
        }else{
            return null;
        }
    }

    public void setId(Integer id) {
        this.id = id;
    }
    public Equipo getEquipo() {
        return equipo;
    }
    public void setEquipo(Equipo e) {
        this.equipo = e;
    }
    public String getNombre() {
        return nombre;
    }
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
    public Integer getDorsal() {
        return dorsal;
    }
    public void setDorsal(Integer dorsal) {
        this.dorsal = dorsal;
    }
    public java.util.Date getTiempo() {
        return tiempo;
    }
    public void setTiempo(java.util.Date tiempo) {
        this.tiempo = tiempo;
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
    public List<Accion> getAcciones() { return new ArrayList<>(acciones); }
    public void agregarAccion(Accion a) { if (a != null) acciones.add(a); }

}