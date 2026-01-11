# Instrucciones para agregar JWT en backend Java

1. Descarga la librería java-jwt de Auth0:
   - Ve a https://github.com/auth0/java-jwt/releases
   - Descarga el archivo JAR más reciente (por ejemplo, `java-jwt-4.4.0.jar`).
   - Coloca el JAR en la carpeta `lib/` de tu backend.

2. Agrega la dependencia en el classpath al compilar y ejecutar:
   - Ejemplo compilación:
     javac -d bin -cp "lib/*" src/WebServer.java
   - Ejemplo ejecución:
     java -cp "bin:lib/*" WebServer

3. Importa las clases necesarias en tu código Java:
   import com.auth0.jwt.JWT;
   import com.auth0.jwt.algorithms.Algorithm;
   import com.auth0.jwt.interfaces.DecodedJWT;
   import com.auth0.jwt.JWTVerifier;

4. Define una clave secreta para firmar los tokens:
   private static final String JWT_SECRET = "tu_clave_secreta_segura";

5. Genera el JWT en el endpoint de login:
   String token = JWT.create()
     .withClaim("curp", curp)
     .withClaim("name", name)
     .withClaim("role", role)
     .withExpiresAt(new Date(System.currentTimeMillis() + 86400000)) // 1 día
     .sign(Algorithm.HMAC256(JWT_SECRET));

6. Valida el JWT en endpoints protegidos:
   JWTVerifier verifier = JWT.require(Algorithm.HMAC256(JWT_SECRET)).build();
   DecodedJWT jwt = verifier.verify(token);
   String curp = jwt.getClaim("curp").asString();
   String name = jwt.getClaim("name").asString();
   String role = jwt.getClaim("role").asString();

7. Reemplaza el "mock-token" por el JWT real en la respuesta del login.

8. En los endpoints protegidos, extrae el token del header Authorization y valida antes de procesar la petición.

¿Listo para que te ayude a modificar el código de WebServer.java para generar y validar JWT?