package ec.edu.espe.bibliotecaapi.controller;

import ec.edu.espe.bibliotecaapi.dto.LibroRequest;
import ec.edu.espe.bibliotecaapi.dto.LibroResponse;
import ec.edu.espe.bibliotecaapi.exception.GlobalExceptionHandler;
import ec.edu.espe.bibliotecaapi.exception.IsbnDuplicadoException;
import ec.edu.espe.bibliotecaapi.exception.LibroNoEncontradoException;
import ec.edu.espe.bibliotecaapi.service.LibroService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LibroControllerTest {
    private MockMvc mvc;
    private LibroService service;
    private static final String LIBRO_VALIDO = """
            {"titulo":"Cien años de soledad","isbn":"9780307474728","stockDisponible":2,"autorId":1}
            """;

    @BeforeEach
    void preparar() {
        service = mock(LibroService.class);
        mvc = MockMvcBuilders.standaloneSetup(new LibroController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void altaDevuelveCreadoConLocationYDto() throws Exception {
        when(service.guardarLibro(any(LibroRequest.class)))
                .thenReturn(new LibroResponse(4L, "Cien años de soledad", "9780307474728", 2, 1L));

        mvc.perform(post("/api/libros").contentType(MediaType.APPLICATION_JSON).content(LIBRO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/libros/4"))
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.stockDisponible").value(2));
    }

    @Test
    void libroInexistenteDevuelve404ConMensajeDeNegocio() throws Exception {
        when(service.obtenerLibro(99L)).thenThrow(new LibroNoEncontradoException(99L));

        mvc.perform(get("/api/libros/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/api/libros/99"))
                .andExpect(jsonPath("$.message").value("No existe el libro con id 99"));
    }

    @Test
    void stockNegativoDevuelve400AntesDeLlamarAlServicio() throws Exception {
        mvc.perform(post("/api/libros").contentType(MediaType.APPLICATION_JSON)
                        .content(LIBRO_VALIDO.replace("\"stockDisponible\":2", "\"stockDisponible\":-1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.stockDisponible").exists());

        verifyNoInteractions(service);
    }

    @Test
    void jsonMalFormadoDevuelve400SinExponerDetallesInternos() throws Exception {
        mvc.perform(post("/api/libros").contentType(MediaType.APPLICATION_JSON).content("{\"titulo\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.trace").doesNotExist());

        verifyNoInteractions(service);
    }

    @Test
    void isbnDuplicadoDevuelve409() throws Exception {
        when(service.guardarLibro(any(LibroRequest.class))).thenThrow(new IsbnDuplicadoException("9780307474728"));

        mvc.perform(post("/api/libros").contentType(MediaType.APPLICATION_JSON).content(LIBRO_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void eliminarDevuelve204SinCuerpo() throws Exception {
        mvc.perform(delete("/api/libros/4"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(service).eliminarLibro(4L);
    }
}
