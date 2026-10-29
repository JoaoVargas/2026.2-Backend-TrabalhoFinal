# Alexandria

Trabalho final da disciplina de Backend. É uma API REST para uma livraria,
construída em **Kotlin + Spring Boot 4**, estendendo o `authserver` visto em
aula (usuários, papéis/roles e autenticação) com um catálogo de livros,
autores, categorias e pedidos.

## Arquitetura

Organização por pacote de domínio (igual ao padrão usado em aula), cada um
com `Entity`, `Repository`, `Service`, `Controller` e DTOs de request/response:

```
br.pucpr.alexandria
├── common      -> GlobalExceptionHandler, exceções genéricas (NotFound, Conflict), SortDir
├── security    -> SecurityConfig (HTTP Basic, stateless), UserDetailsService
├── users       -> usuários (login/senha) e seus papéis
├── roles       -> papéis (ADMIN, CUSTOMER)
├── authors     -> autores dos livros
├── categories  -> categorias/gêneros dos livros
├── books       -> livros (catálogo)
└── orders      -> pedidos de compra (carrinho) e seus itens
```

### Modelo de dados e relacionamentos

| Relação | Tipo |
|---|---|
| `User` ↔ `Role` | **many-to-many** (tabela `UserRole`) |
| `Author` → `Book` | **one-to-many** (um autor tem vários livros) |
| `Book` ↔ `Category` | **many-to-many** (tabela `BookCategory`, sem atributos extras) |
| `User` → `Order` | **one-to-many** (um usuário tem vários pedidos) |
| `Order` ↔ `Book` via `OrderItem` | **many-to-many com tabela no meio**, que carrega `quantity` e o `unitPrice` cobrado na hora da compra |

5 classes novas em relação ao material de aula: `Author`, `Category`, `Book`,
`Order`, `OrderItem`.

### CRUD completo

- **Book**: criar, listar (com filtro/ordenação), buscar por id, atualizar, remover.
- **Author**: criar, listar, buscar por id, atualizar, remover (bloqueado se o autor ainda tiver livros).

### Autenticação e autorização

HTTP Basic, sem sessão (stateless) — o usuário se cadastra em `POST /users` e
passa a usar `email:senha` em todas as chamadas que exigirem login.

- Qualquer `GET` é público (navegar no catálogo não exige login).
- Criar/editar/remover livros, autores, categorias e papéis exige o papel `ADMIN`.
- Criar pedidos e gerenciar os próprios itens exige apenas estar autenticado
  (qualquer usuário logado pode comprar).
- Cancelar um pedido ou remover um item dele só é permitido para o dono do
  pedido ou um `ADMIN` — tentativas de acessar pedido de outra pessoa
  recebem `403 Forbidden`.

Usuário administrador criado automaticamente na primeira inicialização:

```
email: admin@alexandria.com
senha: admin123
```

### Endpoints de associação

- `POST /books/{id}/categories/{categoryId}` e `DELETE /books/{id}/categories/{categoryId}`
  — associa/desassocia uma categoria a um livro.
- `POST /orders/{id}/items` e `DELETE /orders/{id}/items/{itemId}`
  — adiciona/remove um livro de um pedido (ajustando o estoque automaticamente).
- `PUT /users/{id}/roles/{name}` e `DELETE /users/{id}/roles/{name}`
  — concede/revoga um papel de um usuário.

### Filtro e ordenação via query params

`GET /books` aceita `title`, `authorId`, `categoryId`, `minPrice`, `maxPrice`,
`sortBy` (`title`, `price`, `publishedYear`, `stock`) e `sortDir` (`ASC`/`DESC`),
combináveis livremente. Exemplo:

```
GET /api/books?title=hobbit&minPrice=10&maxPrice=100&sortBy=price&sortDir=DESC
```

### Logs e exceções

- `BookService` e `OrderService` logam (SLF4J) as principais operações
  (criação, remoção, reserva/liberação de estoque) e os casos de erro.
- Exceções de negócio dedicadas (`NotFoundException`, `ConflictException` em
  `common`, além de `InvalidSortFieldException`/`InvalidSortDirException`)
  viram respostas `ProblemDetail` consistentes via `GlobalExceptionHandler`,
  incluindo os casos de autenticação (401) e autorização (403).

## Rodando com Docker

Pré-requisito: Docker e Docker Compose.

```bash
docker compose up --build
```

Isso sobe dois containers: `db` (Postgres 16) e `app` (a API). A API fica
disponível em `http://localhost:8081/api` (porta `8081` no host para não
colidir com outros serviços já rodando na 8080 — ajuste em `docker-compose.yml`
se quiser outra porta).

Para derrubar:

```bash
docker compose down        # mantém os dados do Postgres (volume)
docker compose down -v     # remove também o volume do banco
```

## Rodando localmente sem Docker

Usa H2 em memória (perfil padrão), sem precisar de banco externo:

```bash
./gradlew bootRun
```

API em `http://localhost:8080/api`. Console do H2 em `http://localhost:8080/api/h2-console`
(JDBC URL `jdbc:h2:mem:db`, usuário `sa`, senha `sa`).

## Suite de testes automatizada

`test-api.sh` roda uma bateria de ~65 verificações cobrindo cada item (CRUD completo, associações, filtro/ordenação, autenticação e
autorização) diretamente contra a API via curl. Requer `curl` e `jq`.

```bash
./test-api.sh                                    # usa http://localhost:8081/api (docker)
BASE=http://localhost:8080/api ./test-api.sh      # local sem docker
```

Cada execução usa um sufixo único (timestamp) nos dados que cria, então pode
ser rodado várias vezes sem limpar o banco. Ao final imprime quantos testes
passaram/falharam e retorna código de saída não-zero se algo falhar.

## Testando via linha de comando

Ajuste `BASE` conforme o ambiente (`:8081` no Docker, `:8080` local).

```bash
BASE=http://localhost:8081/api
ADMIN="admin@alexandria.com:admin123"

# Catálogo público, com filtro e ordenação
curl "$BASE/books?sortBy=price&sortDir=DESC"

# Ação destrutiva sem autenticação -> 401
curl -i -X DELETE "$BASE/books/1"

# Cadastro de livro exige ADMIN
curl -u "$ADMIN" -X POST "$BASE/books" -H "Content-Type: application/json" \
  -d '{"title":"1984","isbn":"978-0-452-28423-4","price":29.90,"publishedYear":1949,"stock":7,"authorId":1}'

# Associar categoria a um livro
curl -u "$ADMIN" -X POST "$BASE/books/1/categories/1"

# Cadastro de cliente
curl -X POST "$BASE/users" -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","password":"secret123","name":"Alice"}'

# Fluxo de pedido: abrir pedido e adicionar item (consome estoque)
ORDER_ID=$(curl -s -u "alice@example.com:secret123" -X POST "$BASE/orders" | grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*')
curl -u "alice@example.com:secret123" -X POST "$BASE/orders/$ORDER_ID/items" \
  -H "Content-Type: application/json" -d '{"bookId":1,"quantity":2}'

# Cancelar o próprio pedido (devolve o estoque)
curl -u "alice@example.com:secret123" -X DELETE "$BASE/orders/$ORDER_ID"
```
