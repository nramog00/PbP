package stats;

import java.util.ArrayList;
import java.util.Arrays;
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
    private List<Jug> jugadores = new ArrayList<>();

    public Equipo() {
        this.acciones = new ArrayList<>();
        this.jugadores = new ArrayList<>();
    }

    public Equipo(Integer id, String nombre, String abreviatura) {
        this.id = id;
        this.nombre = nombre;
        this.abreviatura = abreviatura;
        this.acciones = new ArrayList<>();
        this.jugadores = new ArrayList<>();
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

    // Getter: Retorna Accion[] (conversión de List a array)
    public Accion[] getAcciones() {
        return acciones != null && !acciones.isEmpty() ? 
               acciones.toArray(new Accion[0]) : // Conversión explícita: List -> Accion[]
               new Accion[0]; // Array vacío si null o vacío
    }
    // Setter: Acepta Accion[] y lo convierte a List
    public void setAcciones(Accion[] accionesArray) {
        if (accionesArray != null) {
            this.acciones = new ArrayList<>(Arrays.asList(accionesArray)); // Conversión: Accion[] -> List
        } else {
            this.acciones = new ArrayList<>();
        }
    }
    // Método para agregar una acción (usando List.add, sin array manual)
    public void agregarAccion(Accion a) {
        if (a != null) {
            acciones.add(a); // Simple y eficiente con List
        }
    }
    // Opcional: Getter para la lista interna (si lo necesitas en otros lugares)
    public List<Accion> getListaAcciones() {
        return new ArrayList<>(acciones); // Retorna copia para evitar modificaciones externas
    }
    // Setter alternativo para List (si lees directamente en lista)
    public void setListaAcciones(List<Accion> lista) {
        if (lista != null) {
            this.acciones = new ArrayList<>(lista);
        } else {
            this.acciones = new ArrayList<>();
        }
    }

    public List<Jug> getJugadores() { return new ArrayList<>(jugadores); }

    // Agregar jugador al equipo
    public void agregarJugador(Jug j) {
        if (j != null && !jugadores.contains(j)) {
            jugadores.add(j);
            j.setEquipo(this); // Bidireccional
        }
    }
}
