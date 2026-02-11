[![Review Assignment Due Date](https://classroom.github.com/assets/deadline-readme-button-22041afd0340ce965d47ae6ef1cefeee28c7c493a6346c4f15d667ab976d596c.svg)](https://classroom.github.com/a/1Xq6ubRH)
# CRUD de tareas en fichero (Jakarta EE + WildFly)

[Enunciado de la práctica (P5.3 - Informe RA3)](docs/DAW-U5-Practica003.md)

Aplicación sencilla con API CRUD y una interfaz web básica para gestionar tareas. Los datos se almacenan en un fichero JSON dentro del directorio de datos de WildFly.

## Funcionalidad
- API REST `CRUD` para tareas.
- Interfaz web básica (HTML + JS) para crear, editar, marcar y borrar.
- Persistencia en fichero JSON.

## Endpoints
- `GET /crud-file/api/tasks`
- `GET /crud-file/api/tasks/{id}`
- `POST /crud-file/api/tasks`
- `PUT /crud-file/api/tasks/{id}`
- `DELETE /crud-file/api/tasks/{id}`

Ejemplo de payload:
```json
{ "title": "Comprar pan", "done": false }
```

## Probar la API con curl
Base URL (si el WAR está desplegado en localhost):
`http://localhost:8080/crud-file/api/tasks`

Listar tareas:
```bash
curl -s http://localhost:8080/crud-file/api/tasks
```

Crear tarea:
```bash
curl -s -X POST http://localhost:8080/crud-file/api/tasks \
  -H "Content-Type: application/json" \
  -d '{ "title": "Comprar pan", "done": false }'
```

Obtener tarea por id:
```bash
curl -s http://localhost:8080/crud-file/api/tasks/1
```

Actualizar tarea:
```bash
curl -s -X PUT http://localhost:8080/crud-file/api/tasks/1 \
  -H "Content-Type: application/json" \
  -d '{ "title": "Comprar leche", "done": true }'
```

Borrar tarea:
```bash
curl -s -X DELETE http://localhost:8080/crud-file/api/tasks/1
```

## Persistencia en fichero
Por defecto se guarda en:
`$JBOSS_SERVER_DATA_DIR/crud-file/tasks.json`

Si necesitas otra ruta, puedes configurar la propiedad de sistema:
`-Dtasks.file=/ruta/absoluta/tasks.json`

## Construcción
Se usa Gradle.
```bash
./gradlew war
```
Si no tienes wrapper:
```bash
gradle war
```
El WAR queda en `build/libs/crud-file.war`.

## Despliegue en WildFly (contenedor con otra app)
Estos pasos siguen el mismo procedimiento de la práctica para levantar WildFly y luego desplegar este WAR en el mismo contenedor.

### Opción A: levantar contenedor desde cero
#### 1) Preparar el entorno Docker
```bash
docker --version

docker pull quay.io/wildfly/wildfly:latest

docker run -d --name wildfly -p 8080:8080 -p 9990:9990 quay.io/wildfly/wildfly:latest

docker ps
```

#### 2) Crear usuario de administración en el contenedor
```bash
docker exec -it wildfly /opt/jboss/wildfly/bin/add-user.sh
```
Elige `Management User` y crea usuario/clave. Si hace falta:
```bash
docker restart wildfly
```
Consola:
- `http://localhost:9990`

#### 3) Desplegar esta aplicación (WAR)
Compila el WAR:
```bash
./gradlew war
```
Copia el WAR al contenedor:
```bash
docker cp build/libs/crud-file.war wildfly:/opt/jboss/wildfly/standalone/deployments/
```

### Opción B: contenedor ya levantado
Si el contenedor ya está activo (por ejemplo con otra app desplegada), solo necesitas compilar y copiar el WAR.

1) Comprueba que el contenedor sigue activo:
```bash
docker ps
```

2) Compila el WAR:
```bash
./gradlew war
```

3) Copia el WAR al contenedor existente:
```bash
docker cp build/libs/crud-file.war wildfly:/opt/jboss/wildfly/standalone/deployments/
```

Tras el despliegue, la aplicación estará disponible en:
- `http://localhost:8080/crud-file/`

## Notas sobre el contenedor
- Si usas un nombre distinto de contenedor, cambia `wildfly` en los comandos.
- Para persistir datos fuera del contenedor, monta `standalone/data` como volumen.
