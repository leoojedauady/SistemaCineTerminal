# Trazabilidad y verificación · Equipo 5

Referencias de alcance: Instrucciones Entrega 2 (3 páginas), Propuesta del
Equipo 5 (6 páginas) y ERS versión 1.0 (15 páginas). La tabla sigue la
numeración de la sección 3.2 del ERS. **CSV** indica funcionalidad disponible
en este corte con el almacenamiento provisional. **Parcial** indica
un comportamiento disponible y condiciones del ERS aún por validar.
**Pendiente** identifica operaciones de la siguiente etapa.

## Requisitos funcionales

| RF | Estado | Clases relacionadas y alcance actual | Caso de aceptación y resultado |
| --- | --- | --- | --- |
| RF-01 Cartelera del día | CSV | MenuCine, VentaServicio, Disponibilidad, Funcion, Pelicula, Sala | Consola: función de hoy con título, duración, clasificación, sala, hora y 32 lugares; tras venta quedan 30. JUnit: otro día produce cartelera vacía. |
| RF-02 Mapa | CSV | MenuCine, VentaServicio, Disponibilidad, Sala | Consola: opción 99 solicita otra; mapa distingue libre, marcado y ocupado al volver tras comprar. |
| RF-03 Selección e importe | CSV | SeleccionAsientos, Sala, Seleccion, VentaServicio, MenuCine | Consola: `A1,A1` rechazado; `A1,A2` muestra dos marcas y total $140.00. JUnit: extremos, inválidos y normalización. |
| RF-04 Confirmar compra | CSV | MenuCine, VentaServicio, RepositorioCompras, AlmacenCsv, Compra | Consola: N seguido de S deja una única compra, con folio 1 y dos lugares. JUnit: folio siguiente distinto, venta en hora exacta y rechazo tras la hora de inicio. |
| RF-05 Emitir boleto | CSV | MenuCine, VentaServicio, Boleto, Compra, Funcion | Consola: folio, película, fecha/hora, sala, dos líneas y total $140.00. JUnit: el boleto coincide al releer el folio tras reabrir. |
| RF-06 Iniciar sesión | CSV | MenuCine, PersonalServicio, Clave, Trabajador, RepositorioTrabajadores | Consola: usuario incorrecto rechazado; cuenta de muestra abre taquilla. JUnit: mismo mensaje para usuario inexistente y clave incorrecta. |
| RF-07 Autorizar y cerrar sesión | CSV | MenuCine, PersonalServicio | Consola: las opciones administrativas se muestran tras autenticación. JUnit: salir revoca consulta y alta. La verificación se ampliará a cada nuevo método de RF-08 a RF-16. |
| RF-08 Venta en mostrador | Pendiente | PersonalServicio y VentaServicio como puntos de extensión | Caso futuro: autenticar, comprar A1 en función de mañana; debe emitir boleto. Sin ejecución en este corte. |
| RF-09 Consultar compra | CSV | PersonalServicio, VentaServicio, RepositorioCompras, Boleto, Compra, MenuCine | Consola: `abc` rechazado, 999 inexistente y 1 recupera una compra vigente. El lector expone el estado histórico almacenado; crear cancelaciones depende de RF-10. |
| RF-10 Cancelación | Pendiente | PersonalServicio y RepositorioCompras como puntos de extensión | Caso futuro: cancelar un folio con A1 y A2, ambos libres y folio consultable como cancelado; repetir y verificar rechazo. Sin ejecución. |
| RF-11 Registrar película | CSV | PersonalServicio, RepositorioCatalogo, AlmacenCsv, ArchivoCsv, Pelicula | Consola: alta con comillas, coma y duración 600, ID 2. JUnit: 1 y 600 aceptados, 0 y 601 rechazados, y persistencia tras reabrir. |
| RF-12 Baja lógica | Pendiente | Pelicula contiene el estado; extender PersonalServicio y RepositorioCatalogo | Caso futuro: desactivar y verificar que la película deja de ofrecerse al programar, con historial intacto. Sin ejecución. |
| RF-13 Registrar función sin empalmes | Pendiente | Funcion contiene intervalo y datos de sala; extender RepositorioCatalogo | Caso futuro: misma sala 16:00–18:00, aceptar 18:00 y rechazar 17:59. Sin ejecución. |
| RF-14 Consultar funciones por fecha | Pendiente | RepositorioCatalogo, Funcion y Compra como base del listado | Caso futuro: fecha dd/mm/aaaa produce lista por sala y hora con ventas; rechazar 31/02. Sin ejecución. |
| RF-15 Eliminar función sin historial | Pendiente | Funcion y Compra se vinculan por ID; extender RepositorioCatalogo | Caso futuro: eliminar función sin folios, rechazar otra con compra cancelada. Sin ejecución. |
| RF-16 Disponibilidad para trabajador | Pendiente | Disponibilidad y VentaServicio como base para una consulta autenticada | Caso futuro: consultar en taquilla una sala de 32 lugares con 2 ocupados, obtener 30 libres y mapa. Sin ejecución para el rol de trabajador. |
| RF-17 Impedir doble venta concurrente | Parcial | RepositorioCompras, AlmacenCsv, VentaServicio | JUnit: dos hilos de una instancia, un ganador; segunda apertura simultánea se rechaza. Caso futuro: terminales independientes compiten en el servidor; exactamente un ganador por ronda. |
| RF-18 Venta múltiple atómica | Parcial | RepositorioCompras, AlmacenCsv, ArchivoCsv, Compra | JUnit: solicitud A1,A2 falla si A2 se adjudicó antes y no ocupa A1 ni deja folio. Caso futuro: inducir fallo JDBC durante la inserción y verificar rollback. |
| RF-19 Hash y sal | CSV | Main, Clave, Trabajador, PersonalServicio, RepositorioTrabajadores, AlmacenCsv | JUnit: misma contraseña para dos cuentas genera sales y hashes distintos; el CSV no contiene la contraseña del fixture. |
| RF-20 Conservar información | Parcial | Main, AlmacenCsv, ArchivoCsv y los tres repositorios | Consola: el CSV conserva compras y asientos mientras dura la sesión y se elimina al salir. JUnit: IDs de películas y folios persisten. Queda migración a BD y verificación integral de las operaciones administrativas pendientes. |

Todos los tipos de producción del UML aparecen en la matriz. Los vínculos
que afectan a RF pendientes denotan piezas existentes donde continuar,
sin atribuirles implementación de esos requisitos. Los accesores, constructores
canónicos y métodos estándar generados por los `record` se omiten del UML.
Las dependencias y realizaciones carecen de multiplicidad en UML. Se anotan
multiplicidades en asociaciones. La referencia `Compra.funcionId` representa
un enlace por identificador. Los repositorios sin miembros en la vista 2
se desarrollan en la vista 3.

## Tres conjuntos JUnit 5

| Conjunto | Normal | Límite | Error |
| --- | --- | --- | --- |
| SeleccionAsientosTest (9) | Dos asientos, orden y normalización | D8 como único lugar y extremo válido | Vacío, duplicado, formato inválido o fuera de sala |
| VentaServicioTest (7) | Venta de dos asientos y consulta tras reapertura | Un asiento en el instante exacto de inicio | Inicio pasado, función inexistente, ocupación sobrevenida y segunda apertura |
| PersonalServicioTest (6) | Login, consulta, alta y verificación de credencial persistida | Duraciones 1 y 600 | Credenciales inválidas, sesiones cerradas, folio inexistente, duración o texto inválidos |

Verificación de este corte en Windows con JDK 21 y Maven 3.9.11:
`mvn clean test` completó las 22 ejecuciones, con 0 fallos,
0 errores y 0 omitidas. Se ejecutó Main en la terminal para verificar la compra, la consulta y el registro en taquilla durante una sesión. Un segundo arranque comprobó que los datos se reinician.
La futura validación de PostgreSQL, la ejecución en Ubuntu y las mediciones de
rendimiento del ERS quedan pendientes.
