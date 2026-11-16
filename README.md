## API REST - Sistema de Biblioteca

**Descripción**

Este proyecto consiste en el desarrollo de una API REST para la gestión de una biblioteca. Permite administrar autores, libros, usuarios y préstamos, empleando una arquitectura por capas y persistencia en base de datos.

**Tecnologías utilizadas**

- Java 21

- Spring Boot

- Spring Data JPA

- Hibernate

- MariaDB

- Maven

- Git y GitHub

**Requisitos previos**

Para ejecutar el proyecto se necesita:

- Java JDK 21.

- Maven.

- XAMPP o un servidor MariaDB.

- Un IDE como IntelliJ IDEA.

**Configuración de la base de datos**

1. lniciar el servicio MySQL/MariaDB desde XAMPP.

2. Crear una base de datos llamada biblioteca_db.

3. Verificar la configuración del archivo src/main/resources/application.properties.

**Configuración utilizada:**

spring.datasource.url=jdbc:mysql://localhost:3306/biblioteca_db?useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=update
spring.jpa.database-platform=org.hibernate.dialect.MariaDBDialect

Si las credenciales de la base de datos son diferentes, deben actualizarse en el archivo de configuración.

**Ejecución del proyecto**

1. Abre el proyecto en IntelliJ IDEA.

2. Verifica que Java 21 esté instalado.

3. Verifica que MariaDB esté en ejecución.

4. Ejecuta la clase principal de Spring Boot.

La aplicación estará disponible en:
http://localhost:8080 

## Endpoints principales

### Autores

- **GET** `/api/autores`: Listar todos los autores.
- **GET** `/api/autores/{id}`: Consultar un autor por su ID.
- **POST** `/api/autores`: Registrar un nuevo autor.
- **PUT** `/api/autores/{id}`: Actualizar los datos de un autor.
- **DELETE** `/api/autores/{id}`: Eliminar un autor.

### Libros

- **GET** `/api/libros`: Listar todos los libros.
- **GET** `/api/libros/disponibles`: Consultar los libros disponibles.
- **GET** `/api/libros/{id}`: Consultar un libro por su ID.
- **POST** `/api/libros`: Registrar un nuevo libro.
- **PUT** `/api/libros/{id}`: Actualizar los datos de un libro.
- **DELETE** `/api/libros/{id}`: Eliminar un libro.

### Usuarios

- **GET** `/api/usuarios`: Listar todos los usuarios.
- **GET** `/api/usuarios/{id}`: Consultar un usuario por su ID.
- **POST** `/api/usuarios`: Registrar un nuevo usuario.
- **PUT** `/api/usuarios/{id}`: Actualizar los datos de un usuario.
- **DELETE** `/api/usuarios/{id}`: Eliminar un usuario.

### Préstamos

- **GET** `/api/prestamos`: Listar todos los préstamos.
- **GET** `/api/prestamos/pendientes`: Consultar los préstamos pendientes.
- **GET** `/api/prestamos/usuario/{usuarioId}`: Consultar los préstamos de un usuario.
- **GET** `/api/prestamos/{id}`: Consultar un préstamo por su ID.
- **POST** `/api/prestamos`: Registrar un nuevo préstamo.
- **PUT** `/api/prestamos/{id}/devolucion`: Registrar la devolución de un libro.

**Manejo de excepciones**

El proyecto utiliza GlobalExceptionHandler junto con @RestControllerAdvice para gestionar errores y devolver las respuestas HTTP adecuadas.

Las excepciones implementadas incluyen:

- LibroNoEncontradoException

- SinEjemplaresException

- PrestamoInvalidoException

## Pruebas realizadas

Se realizaron pruebas funcionales para los recursos de autor, libro, usuario y préstamo mediante solicitudes HTTP en PowerShell.

Se verificaron las operaciones de registro, consulta, préstamo y devolución de libros, incluida la actualización de los ejemplares disponibles.

Repositorio
Proyecto desarrollado de manera colaborativa:

GitHub - Biblioteca API Grupo 3