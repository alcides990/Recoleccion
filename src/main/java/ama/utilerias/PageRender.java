package ama.utilerias;

import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;

public class PageRender<T> {

    private String url;
    private Page<T> page;

    private int totalPaginas;

    private int cantidadRegistro;

    private int paginaActual;

    private List<PageItem> paginas;

    public PageRender(String url, Page<T> page) {
        this.url = url;
        this.page = page;
        this.paginas = new ArrayList<PageItem>();

        cantidadRegistro = page.getSize();
        totalPaginas = page.getTotalPages();
        paginaActual = page.getNumber() + 1;

        int desde, hasta;
        if (totalPaginas <= cantidadRegistro) {
            desde = 1;
            hasta = totalPaginas;
        } else {
            if (paginaActual <= cantidadRegistro / 2) {
                desde = 1;
                hasta = cantidadRegistro;
            } else if (paginaActual >= totalPaginas - cantidadRegistro / 2) {
                desde = totalPaginas - cantidadRegistro + 1;
                hasta = cantidadRegistro;
            } else {
                desde = paginaActual - cantidadRegistro / 2;
                hasta = cantidadRegistro;
            }
        }

        for (int i = 0; i < hasta; i++) {
            paginas.add(new PageItem(desde + i, paginaActual == desde + i));
        }
    }

    public String getUrl() {
        return url;
    }

    public int getTotalPaginas() {
        return totalPaginas;
    }

    public int getPaginaActual() {
        return paginaActual;
    }

    public int getCantidadRegistro() {
        return cantidadRegistro;
    }

    public List<PageItem> getPaginas() {
        return paginas;
    }

    public boolean isFirst() {
        return page.isFirst();
    }

    public boolean isLast() {
        return page.isLast();
    }

    public boolean isHasNext() {
        return page.hasNext();
    }

    public boolean isHasPrevious() {
        return page.hasPrevious();
    }
   
}
