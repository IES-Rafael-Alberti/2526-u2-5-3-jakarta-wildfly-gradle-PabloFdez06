## CAPTURAS REALIZADAS EN PARTE 5.2

se reutilizaran para la parte 5.3 en lo que sea necesario.

![docker-ps](/image.png)
![crear_usuario](/image-1.png)
![acceso_consola](/image-2.png)
![acceso_consola2](/image-3.png)
![logs_deploy](/image-4.png)
![hello](/image-5.png)
![pojo_list](/image-6.png)
![curls](/image-7.png)
![curls_usuarios](/image-8.png)
![instalamos_hey](/image-9.png)
![comando_hey](/image-10.png)

---

**Repo de trabajo original:** https://github.com/PabloFdez06/2526_DAW_u5.2_jakarta-wildfly-gradle_PFF.git

# P5.3 - Informe RA3 sobre el despliegue (WildFly contenedor + Gradle)

## a) Componentes y funcionamiento de los servicios del servidor

En mi despliegue de la P5.2 intervienen varios componentes que trabajan juntos para servir la API REST. Voy a explicar cada uno de ellos y el papel que desempeñan.

### Componentes principales

**Contenedor Docker**: Es el entorno aislado donde se ejecuta todo. Utilizo la /imagen oficial de WildFly (`quay.io/wildfly/wildfly:latest`) que ya viene preconfigurada con todo lo necesario para ejecutar aplicaciones Jakarta EE. El contenedor me permite tener un entorno reproducible y aislado del sistema host.

**WildFly (Servidor de aplicaciones)**: Es el servidor de aplicaciones Jakarta EE que se encarga de ejecutar mi aplicación. WildFly implementa todas las especificaciones de Jakarta EE 10, incluyendo JAX-RS para servicios REST, CDI para inyeccion de dependencias, y Servlet para el manejo de peticiones HTTP.

**Aplicacion WAR**: Es el artefacto que contiene mi aplicacion empaquetada. El archivo `modulename.backend-0.0.1-SNAPSHOT.war` incluye las clases compiladas, los recursos y los descriptores de despliegue. Este WAR se despliega en la carpeta `deployments` de WildFly.

**Puerto 8080**: Es el puerto HTTP donde WildFly escucha las peticiones de los clientes. A traves de este puerto accedo a mi API REST.

**Puerto 9990**: Es el puerto de la consola de administracion de WildFly. Me permite gestionar el servidor, ver metricas y administrar los despliegues de forma grafica.

**Endpoint REST**: Mi servicio REST esta disponible en la ruta `/myproject/module/backend/api/myservice`. Esta compuesto por el context-root definido en `jboss-web.xml`, el application path de `RestApplication.java` y el path del recurso en `MyService.java`.

### Flujo de una peticion

Cuando un cliente hace una peticion a mi endpoint, el flujo es el siguiente:

1. El cliente envia una peticion HTTP a `http://localhost:8080/myproject/module/backend/api/myservice/hello`
2. Docker redirige la peticion del puerto 8080 del host al puerto 8080 del contenedor
3. WildFly recibe la peticion en su subsistema Undertow (servidor web embebido)
4. El contenedor de servlets identifica que la peticion corresponde a mi aplicacion por el context-root `/myproject/module/backend`
5. JAX-RS intercepta la peticion porque coincide con el application path `/api`
6. Se localiza el recurso `MyService` que tiene el path `/myservice`
7. Se invoca el metodo `sayHello()` que tiene el path `/hello`
8. El metodo genera la respuesta y la devuelve al cliente

### Evidencias

Para verificar que el contenedor esta corriendo con los puertos publicados, ejecuto:

```bash
docker ps
```

![docker-ps](/image.png)

Los logs del servidor muestran el despliegue exitoso de la aplicacion:

```bash
docker logs -f wildfly
```

![logs_deploy](/image-4.png)

Y puedo verificar que el endpoint responde correctamente:

```bash
curl http://localhost:8080/myproject/module/backend/api/myservice/hello
```

![hello](/image-5.png)

---

## b) Archivos principales de configuracion y bibliotecas compartidas

### Archivos de configuracion de WildFly

El archivo de configuracion principal de WildFly se encuentra dentro del contenedor en la ruta:

```
/opt/jboss/wildfly/standalone/configuration/standalone.xml
```

Para verificarlo, ejecuto:

```bash
docker exec -it wildfly ls /opt/jboss/wildfly/standalone/configuration/
```

![alt text](/image-12.png)

El archivo `standalone.xml` es el corazon de la configuracion de WildFly en modo standalone. Dentro de este archivo puedo ajustar:

- **Subsistemas**: Configurar el comportamiento de cada modulo de WildFly (datasources, EJB, JAX-RS, etc.)
- **Interfaces de red**: Definir en que direcciones IP escucha el servidor
- **Puertos**: Cambiar los puertos por defecto (8080 para HTTP, 9990 para administracion)
- **Logging**: Configurar niveles de log y handlers
- **Seguridad**: Configurar realms de seguridad y autenticacion
- **Datasources**: Configurar conexiones a bases de datos

Otros archivos importantes en esa carpeta son:
- `standalone-full.xml`: Configuracion completa con todos los subsistemas
- `standalone-ha.xml`: Configuracion para alta disponibilidad
- `mgmt-users.properties`: Usuarios de la consola de administracion
- `application-users.properties`: Usuarios de aplicacion

### Dependencias "provided"

En mi `build.gradle.kts` tengo configurada la dependencia de Jakarta EE como `compileOnly`:

```kotlin
dependencies {
    compileOnly("jakarta.platform:jakarta.jakartaee-api:10.0.0")
}
```

![dependencias](/image-13.png)

Esto significa que estas dependencias solo se usan durante la compilacion, pero NO se incluyen en el WAR final. La razon es que WildFly ya proporciona todas las implementaciones de Jakarta EE 10.

Las ventajas de esto son:

1. **WAR mas ligero**: Mi archivo WAR ocupa mucho menos espacio porque no incluye todas las librerias de Jakarta EE
2. **Sin conflictos de versiones**: Evito problemas de tener dos versiones diferentes de la misma libreria
3. **Mejor rendimiento**: El servidor carga las clases una sola vez y las comparte entre todas las aplicaciones
4. **Actualizaciones centralizadas**: Si actualizo WildFly, todas las aplicaciones se benefician de las mejoras sin recompilar

---

## c) Cooperacion con el servidor web (proxy / reverse proxy) y HTTPS

En mi despliegue de la P5.2 accedo directamente a WildFly por el puerto 8080. Sin embargo, en un entorno de produccion real, lo correcto es poner un servidor web frontal como Nginx delante de WildFly.

### Configuracion de Reverse Proxy con Nginx

Para mi despliegue, configuraria Nginx de la siguiente manera:

```nginx
upstream wildfly_backend {
    server wildfly:8080;
}

server {
    listen 80;
    server_name midominio.com;

    # Redirigir todo el trafico HTTP a HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl;
    server_name midominio.com;

    # Certificados SSL
    ssl_certificate /etc/nginx/ssl/certificado.crt;
    ssl_certificate_key /etc/nginx/ssl/certificado.key;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    # Proxy hacia la API
    location /api/ {
        proxy_pass http://wildfly_backend/myproject/module/backend/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Bloquear acceso a la consola de administracion desde fuera
    location /console {
        deny all;
        return 403;
    }
}
```

### Cambios respecto a mi despliegue actual

Con esta configuracion, los cambios principales serian:

1. **Puertos expuestos**: Solo expondria el puerto 443 (HTTPS) y opcionalmente el 80 para redirigir a HTTPS. El puerto 8080 de WildFly quedaria solo accesible dentro de la red de Docker, no desde el host.

2. **El puerto 9990 nunca se expone**: La consola de administracion solo seria accesible desde dentro del contenedor o mediante un tunel SSH.

3. **URLs mas limpias**: Los clientes accederian a `https://midominio.com/api/myservice/hello` en lugar de la URL larga actual.

4. **En Docker Compose**, publicaria solo los puertos de Nginx:
```yaml
services:
  nginx:
    ports:
      - "80:80"
      - "443:443"
  wildfly:
    # Sin puertos publicados al host
    expose:
      - "8080"
```

### Configuracion de TLS (HTTPS)

Para configurar TLS, necesito obtener certificados SSL. En desarrollo uso certificados autofirmados, pero en produccion usaria Let's Encrypt:

```bash
# Generar certificado autofirmado para desarrollo
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
    -keyout certificado.key \
    -out certificado.crt \
    -subj "/CN=midominio.com"
```

Las ventajas de usar HTTPS son:

1. **Cifrado de datos**: Toda la comunicacion entre cliente y servidor esta cifrada
2. **Autenticidad**: El cliente puede verificar que esta hablando con el servidor correcto
3. **Integridad**: Los datos no pueden ser modificados en transito
4. **SEO y confianza**: Los navegadores marcan como inseguros los sitios sin HTTPS

En cuanto a mi aplicacion y WildFly, no tendria que cambiar nada. La terminacion SSL se hace en Nginx, y la comunicacion interna entre Nginx y WildFly puede seguir siendo HTTP porque estan en la misma red privada de Docker.

---

## d) Mecanismos de seguridad del servidor de aplicaciones

### Medidas aplicadas en P5.2

En mi despliegue de la P5.2 aplique las siguientes medidas basicas:

1. **Creacion de usuario administrador**: Cree un usuario para acceder a la consola de administracion usando el script de WildFly:
```bash
docker exec -it wildfly /opt/jboss/wildfly/bin/add-user.sh
```

![crear_usuario](/image-1.png)

2. **Acceso a la consola solo con credenciales**: La consola del puerto 9990 requiere autenticacion.

### Medidas adicionales para produccion

Para un entorno de produccion, añadiria las siguientes medidas:

**1. No exponer el puerto 9990 al exterior**

En produccion, el puerto de administracion nunca debe ser accesible desde Internet. Solo lo expondria internamente o accederia mediante SSH tunneling:

```bash
# Acceso mediante tunel SSH
ssh -L 9990:localhost:9990 usuario@servidor
```

En Docker, simplemente no publicaria el puerto:
```yaml
wildfly:
  expose:
    - "8080"
  # No incluir 9990 en ports
```

**2. Gestion de secretos con variables de entorno o Docker Secrets**

Nunca hardcodear contraseñas en archivos de configuracion. Usaria Docker Secrets o variables de entorno:

```yaml
services:
  wildfly:
    environment:
      - WILDFLY_ADMIN_USER=${ADMIN_USER}
      - WILDFLY_ADMIN_PASSWORD=${ADMIN_PASSWORD}
    secrets:
      - db_password

secrets:
  db_password:
    file: ./secrets/db_password.txt
```

**3. Configurar logs detallados y centralizados**

Configurar el logging en `standalone.xml` para registrar todos los accesos y errores:

```xml
<subsystem xmlns="urn:jboss:domain:logging:8.0">
    <periodic-rotating-file-handler name="AUDIT">
        <file relative-to="jboss.server.log.dir" path="audit.log"/>
        <suffix value=".yyyy-MM-dd"/>
        <append value="true"/>
    </periodic-rotating-file-handler>
</subsystem>
```

Y usar volumenes para persistir los logs fuera del contenedor:
```yaml
volumes:
  - ./logs:/opt/jboss/wildfly/standalone/log
```

**4. Limitar recursos del contenedor**

Para evitar que un ataque consuma todos los recursos del host:

```yaml
wildfly:
  deploy:
    resources:
      limits:
        cpus: '2'
        memory: 2G
      reservations:
        cpus: '0.5'
        memory: 512M
```

**5. Usar una red Docker aislada**

Crear una red bridge personalizada para aislar los contenedores:

```yaml
networks:
  backend:
    driver: bridge
    internal: true  # Sin acceso a Internet
  frontend:
    driver: bridge
```

![acceso_consola](/image-2.png)
![acceso_consola2](/image-3.png)

---

## e) Componentes web del servidor de aplicaciones

### Que es el WAR y que contiene

El WAR (Web Application Archive) es un archivo comprimido que contiene todo lo necesario para desplegar mi aplicacion web en WildFly. Mi archivo `modulename.backend-0.0.1-SNAPSHOT.war` contiene:

```
modulename.backend-0.0.1-SNAPSHOT.war
├── WEB-INF/
│   ├── classes/
│   │   └── com/mycompany/myproject/module/
│   │       ├── Pojo.class
│   │       ├── RestApplication.class
│   │       └── services/
│   │           └── MyService.class
│   ├── jboss-web.xml
│   └── beans.xml (si existe)
├── META-INF/
│   └── MANIFEST.MF
```

- **WEB-INF/classes**: Contiene las clases Java compiladas de mi aplicacion
- **WEB-INF/jboss-web.xml**: Configuracion especifica de JBoss/WildFly donde defino el context-root
- **META-INF/MANIFEST.MF**: Metadatos del archivo

### Que significa el contexto/ruta base

El contexto o context-root es la ruta base bajo la cual se despliega mi aplicacion en el servidor. En mi caso, esta definido en `jboss-web.xml`:

```xml
<context-root>/myproject/module/backend</context-root>
```

Esto significa que todas las URLs de mi aplicacion empiezan por `/myproject/module/backend`. Si no definiera este archivo, WildFly usaria el nombre del WAR como contexto.

### Que parte del servidor sirve la API

WildFly utiliza varios subsistemas para servir mi API:

1. **Undertow**: Es el servidor web embebido de WildFly. Recibe las peticiones HTTP y las enruta a la aplicacion correcta segun el contexto.

2. **RESTEasy**: Es la implementacion de JAX-RS que usa WildFly. Se encarga de mapear las URLs a los metodos de mis clases REST.

3. **Weld**: Es la implementacion de CDI. Gestiona el ciclo de vida de mis beans, como `MyService` que tiene `@ApplicationScoped`.

### Desglose de URL

Mi URL completa de prueba es:

```
http://localhost:8080/myproject/module/backend/api/myservice/hello
```

Desglosada:

| Parte | Valor | Origen |
|-------|-------|--------|
| Protocolo | `http://` | Protocolo HTTP |
| Host | `localhost` | Maquina local |
| Puerto | `8080` | Puerto HTTP de WildFly |
| Context-root | `/myproject/module/backend` | jboss-web.xml |
| Application path | `/api` | @ApplicationPath en RestApplication.java |
| Resource path | `/myservice` | @Path en MyService.java |
| Method path | `/hello` | @Path en metodo sayHello() |

### Evidencias

![estructura_war](/image-15.png)
---

## f) Parametros necesarios para el despliegue

Para mi despliegue de la P5.2, fueron necesarios varios parametros que detallo a continuacion:

### 1. Parametros de docker run

```bash
docker run -d \
    --name wildfly \
    -p 8080:8080 \
    -p 9990:9990 \
    quay.io/wildfly/wildfly:latest \
    /opt/jboss/wildfly/bin/standalone.sh -b 0.0.0.0 -bmanagement 0.0.0.0
```

- **--name wildfly**: Nombre del contenedor para poder referenciarlo facilmente
- **-p 8080:8080**: Mapeo del puerto HTTP
- **-p 9990:9990**: Mapeo del puerto de administracion
- **-b 0.0.0.0**: Hace que WildFly escuche en todas las interfaces de red
- **-bmanagement 0.0.0.0**: Hace que la consola de administracion sea accesible externamente

**Que pasaria si estuviera mal**: Si no pusiera `-b 0.0.0.0`, WildFly solo escucharia en 127.0.0.1 dentro del contenedor y no podria acceder desde fuera.

### 2. Nombre del WAR

En `build.gradle.kts`:
```kotlin
tasks.withType<War> {
    archiveBaseName.set("modulename.backend")
}
```

Esto genera `modulename.backend-0.0.1-SNAPSHOT.war`.

**Que pasaria si estuviera mal**: Si el nombre no coincide con lo que espero, podria copiar el archivo incorrecto o no encontrarlo.

### 3. Context-root

En `jboss-web.xml`:
```xml
<context-root>/myproject/module/backend</context-root>
```

**Que pasaria si estuviera mal**: La aplicacion se desplegaria en una ruta diferente y las URLs no funcionarian, devolviendo 404.

### 4. Ruta de despliegue

El WAR debe copiarse a:
```bash
docker cp build/libs/modulename.backend-0.0.1-SNAPSHOT.war wildfly:/opt/jboss/wildfly/standalone/deployments/
```

**Que pasaria si estuviera mal**: Si copio el WAR a otra carpeta, WildFly no lo detectaria y no desplegaria la aplicacion.

### 5. Application Path

En `RestApplication.java`:
```java
@ApplicationPath("/api")
```

**Que pasaria si estuviera mal**: Los endpoints REST estarian bajo una ruta diferente. Por ejemplo, si pusiera `/rest` en lugar de `/api`, tendria que usar `/myproject/module/backend/rest/myservice/hello`.

### 6. Version de la /imagen Docker

```bash
quay.io/wildfly/wildfly:latest
```

**Que pasaria si estuviera mal**: Si usara una version antigua que no soporta Jakarta EE 10, la aplicacion no desplegaria porque las clases de javax.* no serian compatibles con jakarta.*.

### 7. Comando para generar el WAR

```bash
./gradlew clean war
```

**Que pasaria si estuviera mal**: Si no ejecuto este comando, no tendria el archivo WAR para desplegar.

![docker-ps](/image.png)
![alt text](/image-16.png)
![alt text](/image-17.png)

---

## g) Pruebas de funcionamiento y rendimiento

### Pruebas funcionales

En la P5.2 realice varias pruebas funcionales para verificar que todos los endpoints funcionan correctamente:

**1. Prueba del endpoint hello**
```bash
curl http://localhost:8080/myproject/module/backend/api/myservice/hello
```
Respuesta esperada: `hello!`

**2. Prueba de listar todos los pojos**
```bash
curl http://localhost:8080/myproject/module/backend/api/myservice/pojo/list
```
Respuesta esperada: JSON con array de pojos

**3. Prueba de buscar un pojo por ID**
```bash
curl http://localhost:8080/myproject/module/backend/api/myservice/pojo/find/3
```
Respuesta esperada: JSON con el pojo de id 1

**4. Prueba de crear un nuevo pojo**
```bash
curl -X POST -H "Content-Type: application/json" \
    -d '{"id": 3, "name": "NuevoPojo"}' \
    http://localhost:8080/myproject/module/backend/api/myservice/pojo/new
```
Respuesta esperada: 201 Created

**5. Prueba de actualizar un pojo**
```bash
curl -X PUT -H "Content-Type: application/json" \
    -d '{"id": 3, "name": "Actualizado"}' \
    http://localhost:8080/myproject/module/backend/api/myservice/pojo/update
```
Respuesta esperada: 204 No Content

**6. Prueba de eliminar un pojo**
```bash
curl -X DELETE "http://localhost:8080/myproject/module/backend/api/myservice/pojo/remove?id=3"
```
Respuesta esperada: 204 No Content

![pruebas_curls](/image-18.png)

### Prueba de rendimiento con hey

Para la prueba de rendimiento utilice la herramienta `hey`. Primero la instale:

```bash
sudo apt-get install hey
```

Luego ejecute la prueba con 1000 peticiones y 10 conexiones concurrentes:

```bash
hey -n 1000 -c 10 http://localhost:8080/myproject/module/backend/api/myservice/hello
```

**Analisis del resultado:**

- **Rendimiento excelente**: El servidor procesa 8262 peticiones por segundo, lo cual es muy bueno para un endpoint simple.
- **Latencia baja**: El tiempo medio de respuesta es de 1.2 milisegundos, con el 50% de las peticiones respondidas en menos de 0.9ms.
- **Sin errores**: Las 1000 peticiones devolvieron codigo 200, sin ningun error.
- **Consistencia**: La diferencia entre la peticion mas rapida (0.0005ms) y la mas lenta (0.03ms) es aceptable.

![comando-hey](/image-19.png)

---

## h) Documentacion de administracion y recomendaciones

### Mini-guia de administracion del despliegue

Esta guia describe como administrar el despliegue de la API REST en WildFly usando Docker.

---

#### 1. Como levantar WildFly

**Opcion A: Usando docker run**

```bash
docker run -d \
    --name wildfly \
    -p 8080:8080 \
    -p 9990:9990 \
    quay.io/wildfly/wildfly:latest \
    /opt/jboss/wildfly/bin/standalone.sh -b 0.0.0.0 -bmanagement 0.0.0.0
```

**Opcion B: Usando docker compose (recomendado)**

```bash
docker compose up -d
```

Para verificar que el contenedor esta corriendo:
```bash
docker ps
```

---

#### 2. Como desplegar una nueva version del WAR

**Paso 1**: Compilar el proyecto con Gradle
```bash
./gradlew clean war
```

**Paso 2**: Verificar que se genero el WAR
```bash
ls build/libs/
```
Debe aparecer `modulename.backend-0.0.1-SNAPSHOT.war`

**Paso 3**: Copiar el WAR al contenedor
```bash
docker cp build/libs/modulename.backend-0.0.1-SNAPSHOT.war wildfly:/opt/jboss/wildfly/standalone/deployments/
```

**Paso 4**: Verificar el despliegue en los logs
```bash
docker logs -f wildfly
```
Buscar el mensaje: `Deployed "modulename.backend-0.0.1-SNAPSHOT.war"`

---

#### 3. Como comprobar el estado

**Ver logs del servidor:**
```bash
docker logs -f wildfly
```

**Probar endpoint de prueba:**
```bash
curl http://localhost:8080/myproject/module/backend/api/myservice/hello
```
Respuesta esperada: `hello!`

**Ver estado del contenedor:**
```bash
docker ps
docker stats wildfly
```

**Acceder a la consola de administracion:**
- URL: http://localhost:9990
- Usuario: admin (o el que hayas creado)
- Password: el que configuraste

---

#### 4. Comandos utiles

| Accion | Comando |
|--------|---------|
| Parar contenedor | `docker stop wildfly` |
| Iniciar contenedor | `docker start wildfly` |
| Reiniciar contenedor | `docker restart wildfly` |
| Ver logs | `docker logs -f wildfly` |
| Entrar al contenedor | `docker exec -it wildfly bash` |
| Eliminar contenedor | `docker rm -f wildfly` |

---

#### 5. Recomendaciones para evitar errores comunes

**Error: 404 Not Found**
- Verificar que el WAR se desplego correctamente (mirar logs)
- Comprobar que la URL es correcta: `/myproject/module/backend/api/myservice/...`
- Asegurarse de que el context-root en jboss-web.xml es correcto

**Error: Connection refused**
- Verificar que el contenedor esta corriendo: `docker ps`
- Comprobar que los puertos estan mapeados correctamente
- Asegurar que WildFly arranco con `-b 0.0.0.0`

**Error: El WAR no se despliega**
- Mirar los logs para ver errores de compilacion
- Verificar que las dependencias son compatibles con Jakarta EE 10
- Comprobar que no hay errores de sintaxis en el codigo

**Error: No puedo acceder a la consola 9990**
- Verificar que arrancaste con `-bmanagement 0.0.0.0`
- Comprobar que creaste un usuario administrador
- Asegurarte de que el puerto 9990 esta mapeado

---

#### 6. URLs de prueba

| Endpoint | URL |
|----------|-----|
| Hello | http://localhost:8080/myproject/module/backend/api/myservice/hello |
| Listar pojos | http://localhost:8080/myproject/module/backend/api/myservice/pojo/list |
| Buscar pojo | http://localhost:8080/myproject/module/backend/api/myservice/pojo/find/1 |
| Consola admin | http://localhost:9990 |

---

## i) Virtualizacion, nube o contenedores en el despliegue

Para este apartado voy a crear un despliegue completo usando Docker Compose que incluya Nginx como servidor web frontal y WildFly como servidor de aplicaciones.

### Arquitectura del despliegue

```
                    Internet
                        |
                   [Puerto 80/443]
                        |
                    +-------+
                    | Nginx |  (Reverse Proxy + SSL)
                    +-------+
                        |
                   [Red interna]
                        |
                   +----------+
                   | WildFly  |  (Servidor de aplicaciones)
                   +----------+
                        |
                  [Volumenes]
                   /    |    \
               logs  config  deployments
```

### Estructura de archivos

```
proyecto/
├── docker-compose.yml
├── nginx/
│   ├── nginx.conf
│   └── ssl/
│       ├── certificado.crt
│       └── certificado.key
├── wildfly/
│   └── deployments/
│       └── modulename.backend-0.0.1-SNAPSHOT.war
├── logs/
│   ├── nginx/
│   └── wildfly/
└── secrets/
    └── wildfly_admin_password.txt
```

### docker-compose.yml

```yaml
version: '3.8'

services:
  nginx:
    /image: nginx:alpine
    container_name: nginx-proxy
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
      - ./nginx/ssl:/etc/nginx/ssl:ro
      - ./logs/nginx:/var/log/nginx
    depends_on:
      wildfly:
        condition: service_healthy
    networks:
      - frontend
      - backend
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "nginx", "-t"]
      interval: 30s
      timeout: 10s
      retries: 3

  wildfly:
    /image: quay.io/wildfly/wildfly:latest
    container_name: wildfly-app
    command: /opt/jboss/wildfly/bin/standalone.sh -b 0.0.0.0 -bmanagement 0.0.0.0
    expose:
      - "8080"
      - "9990"
    volumes:
      - ./wildfly/deployments:/opt/jboss/wildfly/standalone/deployments
      - ./logs/wildfly:/opt/jboss/wildfly/standalone/log
    networks:
      - backend
    restart: unless-stopped
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 2G
        reservations:
          cpus: '0.5'
          memory: 512M
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/myproject/module/backend/api/myservice/hello"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 60s
    secrets:
      - wildfly_admin_password

networks:
  frontend:
    driver: bridge
  backend:
    driver: bridge
    internal: true

secrets:
  wildfly_admin_password:
    file: ./secrets/wildfly_admin_password.txt
```

### nginx.conf

```nginx
events {
    worker_connections 1024;
}

http {
    upstream wildfly_backend {
        server wildfly-app:8080;
    }

    # Redirigir HTTP a HTTPS
    server {
        listen 80;
        server_name localhost;
        return 301 https://$host$request_uri;
    }

    # Servidor HTTPS
    server {
        listen 443 ssl;
        server_name localhost;

        ssl_certificate /etc/nginx/ssl/certificado.crt;
        ssl_certificate_key /etc/nginx/ssl/certificado.key;
        ssl_protocols TLSv1.2 TLSv1.3;
        ssl_ciphers HIGH:!aNULL:!MD5;

        # Logs
        access_log /var/log/nginx/access.log;
        error_log /var/log/nginx/error.log;

        # API REST
        location /api/ {
            proxy_pass http://wildfly_backend/myproject/module/backend/api/;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

        # Healthcheck
        location /health {
            proxy_pass http://wildfly_backend/myproject/module/backend/api/myservice/hello;
        }

        # Bloquear acceso a consola de administracion
        location /console {
            deny all;
            return 403;
        }
    }
}
```

### Caracteristicas implementadas

**1. Volumenes para logs**
- Los logs de Nginx se guardan en `./logs/nginx`
- Los logs de WildFly se guardan en `./logs/wildfly`
- Esto permite acceder a los logs sin entrar al contenedor y persisten aunque se elimine el contenedor

**2. Secretos**
- La contraseña del administrador de WildFly se gestiona como Docker Secret
- El archivo esta en `./secrets/wildfly_admin_password.txt`
- No se expone en variables de entorno ni en el docker-compose

**3. Healthchecks**
- Nginx verifica que su configuracion es valida
- WildFly verifica que el endpoint `/hello` responde correctamente
- Si el healthcheck falla, Docker puede reiniciar el contenedor

**4. Reinicio automatico**
- `restart: unless-stopped` hace que los contenedores se reinicien automaticamente si fallan o si se reinicia el host

**5. Limitacion de recursos**
- WildFly tiene un limite de 2 CPUs y 2GB de RAM
- Esto evita que consuma todos los recursos del host

**6. Redes separadas**
- `frontend`: Conecta Nginx con el exterior
- `backend`: Red interna entre Nginx y WildFly, sin acceso a Internet

**7. Backend no expuesto**
- WildFly usa `expose` en lugar de `ports`, por lo que solo es accesible desde la red interna de Docker
- El puerto 9990 de administracion no es accesible desde fuera

### Comandos para usar el despliegue

**Levantar todo:**
```bash
docker compose up -d
```

**Ver logs:**
```bash
docker compose logs -f
```

**Parar todo:**
```bash
docker compose down
```

**Probar la API (ahora con HTTPS):**
```bash
curl -k https://localhost/api/myservice/hello
```

**Ver estado de los contenedores:**
```bash
docker compose ps
```