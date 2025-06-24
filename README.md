
# MCloud - Authenticator Back-End (Spring Boot)

Este projeto é o serviço de autenticação da plataforma **MCloud**, desenvolvido em Java com Spring Boot. Ele utiliza Auth0 para autenticação via OAuth2, e banco de dados PostgreSQL para persistência.

---

## 🚀 Tecnologias Utilizadas

- Java 17
- Spring Boot 3.x
- Spring Security (OAuth2 + JWT)
- PostgreSQL
- Docker / Docker Compose
- Maven Wrapper
- Auth0 (OpenID Connect)

---

## ⚙️ Como Funciona o Auth0 na Aplicação

A aplicação usa Auth0 como provedor externo de identidade. Os usuários se autenticam via Auth0, que emite um token JWT. Esse token é enviado nas requisições para acessar os recursos protegidos da API.

A aplicação valida esse token usando a configuração do issuer e audience, garantindo a segurança das operações.

---

## 📌 Principais Endpoints

Documentação Swagger com os principais endpoints expostos pela API de autenticação:

### `Swagger`
http://localhost:8080/swagger-ui/index.html
---

## 🧪 Como Executar Localmente com Docker
**Clone o projeto:**
```bash
git clone https://github.com/amalmeida/mcloud-back-springboot.git
cd mcloud-back-springboot
```

1. Gere os binários com Maven wrapper:
```bash
./mvnw clean package
```

2. **Suba os containers:**
```bash
docker-compose up --build
```

A aplicação estará acessível em `http://localhost:8080`.

---

## 📦 Build Manual (sem Docker)

```bash
./mvnw clean package -DskipTests
java -jar target/*.jar
```

---

## 👥 Contribuindo

1. Crie uma branch:
```bash
git checkout -b feature/sua-feature
```

2. Faça suas alterações e commit:
```bash
git commit -m "feat: sua melhoria"
```

3. Envie para o repositório:
```bash
git push origin feature/sua-feature
```

4. Crie um Pull Request no GitHub.

---

## 📬 Contato

Projeto mantido por [André Massafra Almeida](https://www.linkedin.com/in/andre-massafra-almeida/)  
📧 Email: **andremassafraalmeida@gmail.com**
