# Entrega 2 · Sistema de Gestión y Venta de Boletos para un Cine

**Universidad Autónoma de Yucatán · Facultad de Matemáticas**  
**Construcción de Software · Equipo 5**

## Avance del proyecto

El prototipo de consola se desarrolló en Java 21 con Maven. Cubre el flujo del
cliente desde la cartelera hasta el boleto, incluida la selección de asientos,
el cálculo del importe y la confirmación de compra. El trabajador puede iniciar
y cerrar sesión, consultar una compra por folio y registrar películas.

Se cubren **10 de los 20 requisitos funcionales** en el alcance del prototipo.
Otros tres tienen cobertura parcial y siete están pendientes. Los datos de
demostración se generan en CSV para cada ejecución, lo que permite revisar
compras y altas durante la misma sesión. Los repositorios permiten sustituir
ese almacenamiento por PostgreSQL en una etapa posterior.

## Responsabilidades y continuación

La asignación corresponde a la propuesta del Equipo 5. Los avances se
presentan por módulo y la revisión, integración y documentación son
responsabilidades compartidas.

| Integrante | Avance del módulo en esta entrega | Trabajo de la siguiente etapa |
| --- | --- | --- |
| Domínguez Franco César Adrián | Repositorios desacoplados y lectura y escritura de datos temporales en CSV. | Elaborar el diagrama entidad-relación y el esquema. Implementar la conexión y los repositorios PostgreSQL con JDBC. |
| Ek May Víctor Saúl | Alta de películas y modelo de funciones para la cartelera. | Agregar baja lógica, registro y consulta de funciones, cálculo de término, control de empalmes y eliminación protegida. |
| Lizama Solís Edrei Enrique | Mapa, selección y cálculo del importe, compra del cliente, folios y boletos. | Completar venta y disponibilidad en mostrador y validar la compra atómica ante solicitudes simultáneas. |
| Ojeda Vales Leonardo | Menús, cartelera, validación de entradas y acceso de trabajador con contraseña protegida mediante hash y sal. | Conectar las nuevas opciones de trabajador y verificar la autorización de cada operación. |
| Yupit Collí Rodrigo Irineo | Consulta por folio, integración y pruebas de la lógica del prototipo. | Añadir cancelación con liberación de asientos, ampliar la verificación e integrar los cambios del equipo. |

## Verificación y materiales de revisión

Se verificó en Windows con JDK 21. Las **22 pruebas JUnit 5** pasaron.
Se agrupan en selección de asientos, compra y operaciones de trabajador,
con casos normales, límite y de error. El Javadoc se encuentra en los
comentarios junto al código fuente.

- [Diagrama de clases](Docs/DiagramaClases_Equipo.pdf) y
  [fuente editable](Docs/DiagramaClases_Equipo.puml).
- [Matriz de trazabilidad y pruebas](Docs/Trazabilidad.md).

Para revisar la consola desde la raíz del proyecto, ejecutar:

```sh
mvn compile
java -cp target/classes app.Main
```

La cuenta de demostración es **taquilla** con contraseña **CineDemo2026**.
Las pruebas se ejecutan con `mvn test`.
