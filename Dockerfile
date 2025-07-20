# 1️⃣ Build réteg: Az alkalmazás buildelése egy teljes JDK környezetben
FROM eclipse-temurin:24-jdk-alpine AS build

# Munka könyvtár beállítása
WORKDIR /app

# Maven wrapper és forráskód másolása
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
COPY src ./src

# Maven build futtatása
RUN ./mvnw clean package -DskipTests

# 2️⃣ Runtime réteg: A kész alkalmazás egy kisebb JRE környezetben
FROM eclipse-temurin:24-jre-alpine

# Nem root felhasználó létrehozása és beállítása
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Munka könyvtár létrehozása és jogok beállítása
WORKDIR /app
COPY --from=build /app/target/*.jar srcprofit_app.jar
RUN chown -R appuser:appgroup /app

# Felhasználóváltás (root ➝ appuser)
USER appuser

# Port megnyitása
EXPOSE 8080

# Alkalmazás futtatása
CMD ["java", "-jar", "srcprofit_app.jar"]