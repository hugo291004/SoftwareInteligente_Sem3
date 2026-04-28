# Proceso de Configuración y Arranque del Proyecto

**Sistema Operativo:** Ubuntu 22.04 LTS  
**Java:** 17  

---

## 1. Configuración de Máquinas Virtuales (VMs)
Configure dos máquinas virtuales utilizando la configuración de red **Adaptador Puente (Bridged Adapter)**. Esto habilita la comunicación local y directa entre ambas plataformas dentro de la misma red.

---

## 2. Plataforma 1: Nodo Principal (Scrum Master y Product Owner)
En esta máquina residirán los agentes `ScrumMaster`, `ProductOwner` y `Task`.

### 2.1 Configuración del Product Owner
El agente `ProductOwner` espera leer un archivo CSV (por ejemplo, `backlog.csv`) con la siguiente estructura:

```csv
Login,high,2026-05-01
Register,medium,2026-05-03
Payment,high,2026-05-02
Profile,low,2026-05-10
```

### 2.2 Configuración del Scrum Master
> **⚠️ Importante:** El agente `ScrumMaster` posee las direcciones IP de los developers hardcodeadas. Cambie esas direcciones por la dirección de la máquina virtual 2 en el código fuente antes de compilar.

### 2.3 Compilación


```bash
javac -cp .:../lib/jade.jar *.java
```

### 2.4 Configuración de Red (RMI)
Indique a Java qué IP utilizar para los objetos remotos, forzando el uso de la IP de la red puente para evitar errores de conexión (en lugar de conectarse a localhost).

```bash
export JAVA_OPTS="-Djava.rmi.server.hostname=<DIRECCION_MAQUINA_1>"
```

### 2.5 Ejecución del Main Container y Scrum Master
Abre los módulos internos de Java 17 (`--add-opens`), establezca la IP de su máquina virtual, habilite la comunicación HTTP entre plataformas (`-mtp`) y arranque el agente.

```bash
java --add-opens java.xml/com.sun.org.apache.xerces.internal.jaxp=ALL-UNNAMED \
     -cp ../lib/jade.jar:. jade.Boot -gui \
     -host <DIRECCION_MAQUINA_1> -port 1099 \
     -mtp "jade.mtp.http.MessageTransportProtocol(http://<DIRECCION_MAQUINA_1>:7778/acc)" \
     ScrumMaster:ScrumMasterAgent
```

### 2.6 Ejecución del Product Owner


```bash
java -cp ../lib/jade.jar:. jade.Boot -container "ProductOwner:ProductOwner(backlog.csv)"
```

---

## 3. Plataforma 2: Nodo de Desarrolladores
En esta segunda máquina se alojarán los agentes desarrolladores (`Dev1` y `Dev2`). Asegúrese de compilar primero usando el mismo comando del paso 2.3.

### 3.1 Ejecución de los Developers
Similar al nodo principal.

```bash
java --add-opens java.xml/com.sun.org.apache.xerces.internal.jaxp=ALL-UNNAMED \
     -cp ../lib/jade.jar:. jade.Boot -gui \
     -host <DIRECCION_MAQUINA_2> -port 1099 \
     -mtp "jade.mtp.http.MessageTransportProtocol(http://<DIRECCION_MAQUINA_2>:7778/acc)" \
     Dev1:DeveloperAgent Dev2:DeveloperAgent
```
