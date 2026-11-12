const fs = require('node:fs');
const path = require('node:path');

const root = path.resolve(__dirname, '..');

function read(relativePath) {
  return fs.readFileSync(path.join(root, relativePath), 'utf8');
}

function write(relativePath, content) {
  const target = path.join(root, relativePath);
  fs.mkdirSync(path.dirname(target), { recursive: true });
  const normalized = relativePath.endsWith('.xml') ? content.replace(/\n\+/g, '\n') : content;
  fs.writeFileSync(target, normalized);
}

function replaceRequired(content, searchValue, replacement, label) {
  if (!content.includes(searchValue)) {
    throw new Error(`Не найден ожидаемый фрагмент Swagger Codegen: ${label}`);
  }
  return content.replace(searchValue, replacement);
}

let ticketPom = read('ticket-service/pom.xml');
ticketPom = replaceRequired(ticketPom, '<packaging>jar</packaging>', '<packaging>war</packaging>', 'ticket packaging');
ticketPom = replaceRequired(
  ticketPom,
  `        <dependency>\n            <groupId>org.springframework.boot</groupId>\n            <artifactId>spring-boot-starter-tomcat</artifactId>\n            <version>\${springboot-version}</version>\n        </dependency>`,
  `        <dependency>\n            <groupId>org.springframework.boot</groupId>\n            <artifactId>spring-boot-starter-tomcat</artifactId>\n            <version>\${springboot-version}</version>\n            <scope>provided</scope>\n        </dependency>`,
  'provided Tomcat'
);
write('ticket-service/pom.xml', ticketPom);

let springApplication = read('ticket-service/src/main/java/ru/ifmo/soa/ticket/OpenAPISpringBoot.java');
springApplication = replaceRequired(
  springApplication,
  'import org.springframework.boot.autoconfigure.SpringBootApplication;',
  `import org.springframework.boot.autoconfigure.SpringBootApplication;\nimport org.springframework.boot.builder.SpringApplicationBuilder;\nimport org.springframework.boot.web.servlet.support.SpringBootServletInitializer;`,
  'SpringBootServletInitializer imports'
);
springApplication = replaceRequired(
  springApplication,
  'public class OpenAPISpringBoot implements CommandLineRunner {',
  'public class OpenAPISpringBoot extends SpringBootServletInitializer implements CommandLineRunner {',
  'SpringBootServletInitializer base class'
);
springApplication = replaceRequired(
  springApplication,
  '    @Override\n    public void run(String... arg0) throws Exception {',
  `    @Override\n    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {\n        return application.sources(OpenAPISpringBoot.class);\n    }\n\n    @Override\n    public void run(String... arg0) throws Exception {`,
  'WAR configure method'
);
write('ticket-service/src/main/java/ru/ifmo/soa/ticket/OpenAPISpringBoot.java', springApplication);

write('ticket-service/src/main/webapp/WEB-INF/jboss-web.xml', `<jboss-web xmlns="http://www.jboss.com/xml/ns/javaee"\n+           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"\n+           xsi:schemaLocation="http://www.jboss.com/xml/ns/javaee http://www.jboss.org/j2ee/schema/jboss-web_7_2.xsd"\n+           version="7.2">\n+    <context-root>/api/v1</context-root>\n+</jboss-web>\n`);

let jacksonConfig = read('booking-service/src/gen/java/ru/ifmo/soa/booking/JacksonConfig.java');
jacksonConfig = replaceRequired(
  jacksonConfig,
  '            .setDateFormat(new RFC3339DateFormat())\n',
  '            .setDateFormat(new RFC3339DateFormat());\n',
  'JacksonConfig semicolon'
);
write('booking-service/src/gen/java/ru/ifmo/soa/booking/JacksonConfig.java', jacksonConfig);

write('booking-service/pom.xml', `<project xmlns="http://maven.apache.org/POM/4.0.0"\n+         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"\n+         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">\n+    <modelVersion>4.0.0</modelVersion>\n+    <groupId>ru.ifmo.soa</groupId>\n+    <artifactId>booking-service</artifactId>\n+    <version>1.0.0</version>\n+    <packaging>war</packaging>\n+    <name>booking-service</name>\n+\n+    <properties>\n+        <maven.compiler.release>17</maven.compiler.release>\n+        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>\n+        <swagger-annotations.version>2.2.25</swagger-annotations.version>\n+        <jackson.version>2.17.2</jackson.version>\n+    </properties>\n+\n+    <dependencies>\n+        <dependency>\n+            <groupId>jakarta.platform</groupId>\n+            <artifactId>jakarta.jakartaee-api</artifactId>\n+            <version>10.0.0</version>\n+            <scope>provided</scope>\n+        </dependency>\n+        <dependency>\n+            <groupId>io.swagger.core.v3</groupId>\n+            <artifactId>swagger-annotations-jakarta</artifactId>\n+            <version>\${swagger-annotations.version}</version>\n+        </dependency>\n+        <dependency>\n+            <groupId>com.fasterxml.jackson.core</groupId>\n+            <artifactId>jackson-databind</artifactId>\n+            <version>\${jackson.version}</version>\n+        </dependency>\n+    </dependencies>\n+\n+    <build>\n+        <finalName>booking</finalName>\n+        <plugins>\n+            <plugin>\n+                <groupId>org.codehaus.mojo</groupId>\n+                <artifactId>build-helper-maven-plugin</artifactId>\n+                <version>3.6.0</version>\n+                <executions>\n+                    <execution>\n+                        <id>add-generated-sources</id>\n+                        <phase>generate-sources</phase>\n+                        <goals><goal>add-source</goal></goals>\n+                        <configuration>\n+                            <sources><source>src/gen/java</source></sources>\n+                        </configuration>\n+                    </execution>\n+                </executions>\n+            </plugin>\n+            <plugin>\n+                <groupId>org.apache.maven.plugins</groupId>\n+                <artifactId>maven-war-plugin</artifactId>\n+                <version>3.4.0</version>\n+                <configuration><failOnMissingWebXml>false</failOnMissingWebXml></configuration>\n+            </plugin>\n+        </plugins>\n+    </build>\n+</project>\n`);

write('booking-service/src/main/webapp/WEB-INF/jboss-web.xml', `<jboss-web xmlns="http://www.jboss.com/xml/ns/javaee"\n+           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"\n+           xsi:schemaLocation="http://www.jboss.com/xml/ns/javaee http://www.jboss.org/j2ee/schema/jboss-web_7_2.xsd"\n+           version="7.2">\n+    <context-root>/booking</context-root>\n+</jboss-web>\n`);

write('booking-service/src/main/webapp/WEB-INF/web.xml', `<?xml version="1.0" encoding="UTF-8"?>\n+<web-app xmlns="https://jakarta.ee/xml/ns/jakartaee"\n+         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"\n+         xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-app_6_0.xsd"\n+         version="6.0">\n+    <filter>\n+        <filter-name>ApiOriginFilter</filter-name>\n+        <filter-class>ru.ifmo.soa.booking.api.ApiOriginFilter</filter-class>\n+    </filter>\n+    <filter-mapping>\n+        <filter-name>ApiOriginFilter</filter-name>\n+        <url-pattern>/*</url-pattern>\n+    </filter-mapping>\n+</web-app>\n`);

for (const relativePath of [
  'ticket-service/src/main/java/ru/ifmo/soa/ticket/model/Coordinates.java',
  'ticket-service/src/main/java/ru/ifmo/soa/ticket/model/Ticket.java',
  'ticket-service/src/main/java/ru/ifmo/soa/ticket/model/TicketInput.java',
  'ticket-service/src/main/java/ru/ifmo/soa/ticket/model/TicketPatch.java',
  'booking-service/src/gen/java/ru/ifmo/soa/booking/api/SellApi.java'
]) {
  let content = read(relativePath);
  content = content.replace('@DecimalMin("-239")', '@DecimalMin(value = "-239", inclusive = false)');
  content = content.replace('@Min(-495L)', '@Min(-494L)');
  content = content.replace('@DecimalMin("0")', '@DecimalMin(value = "0", inclusive = false)');
  write(relativePath, content);
}

console.log('Adapted generated projects for Jakarta EE 10 and external WildFly deployment.');
