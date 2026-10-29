#!/usr/bin/env bash
# Suite de testes de ponta a ponta para a API Alexandria.
# Exercita cada item da rubrica via curl: CRUD completo, associações,
# filtro/ordenação, autenticação e autorização.
#
# Uso:
#   ./test-api.sh                 # usa http://localhost:8081/api (docker)
#   BASE=http://localhost:8080/api ./test-api.sh   # local sem docker
#
# Requer: curl, jq

set -uo pipefail

BASE="${BASE:-http://localhost:8081/api}"
ADMIN="admin@alexandria.com:admin123"
RUN_ID=$(date +%s)

PASS=0
FAIL=0
STATUS=""
BODY=""

# --- helpers -----------------------------------------------------------

section() { echo; echo "== $1 =="; }

pass() { echo "  OK   $1"; PASS=$((PASS + 1)); }
fail() { echo "  FAIL $1"; FAIL=$((FAIL + 1)); }

expect_status() {
    local expected="$1" desc="$2"
    if [ "$STATUS" = "$expected" ]; then
        pass "$desc -> $STATUS"
    else
        fail "$desc -> esperado $expected, recebido $STATUS | body=$BODY"
    fi
}

expect_eq() {
    local actual="$1" expected="$2" desc="$3"
    if [ "$actual" = "$expected" ]; then
        pass "$desc"
    else
        fail "$desc (esperado '$expected', obtido '$actual')"
    fi
}

# call METHOD PATH [AUTH] [JSON_BODY]
# preenche as variáveis globais STATUS e BODY
call() {
    local method="$1" path="$2" auth="${3:-}" data="${4:-}"
    local args=(-s -w '\n%{http_code}' -X "$method" "$BASE$path")
    [ -n "$auth" ] && args+=(-u "$auth")
    [ -n "$data" ] && args+=(-H "Content-Type: application/json" -d "$data")

    local out
    out=$(curl "${args[@]}")
    STATUS="${out##*$'\n'}"
    BODY="${out%$'\n'*}"
}

jq_get() { echo "$BODY" | jq -r "$1"; }

urlencode() { jq -rn --arg v "$1" '$v|@uri'; }

require_tools() {
    command -v curl >/dev/null || { echo "curl não encontrado"; exit 1; }
    command -v jq >/dev/null || { echo "jq não encontrado (sudo apt install jq / pacman -S jq)"; exit 1; }
}

# --- 0. disponibilidade --------------------------------------------------

require_tools
section "0. Health check"
call GET /health
expect_status 200 "GET /health"
expect_eq "$(jq_get '.status')" "OK" "payload de /health contém status OK"

# --- 1. catálogo público (sem autenticação) ------------------------------

section "1. Catálogo é público (GET sem credenciais)"
call GET /books
expect_status 200 "GET /books sem auth"
call GET /authors
expect_status 200 "GET /authors sem auth"
call GET /categories
expect_status 200 "GET /categories sem auth"
call GET /roles
expect_status 200 "GET /roles sem auth"

# --- 2. autenticação: ações destrutivas exigem credenciais ----------------

section "2. Autenticação em ações destrutivas"
call DELETE /books/999999
expect_status 401 "DELETE sem credenciais -> 401"
call DELETE /books/999999 "admin@alexandria.com:senha-errada"
expect_status 401 "DELETE com senha errada -> 401"

# --- 3. CRUD completo: Author ---------------------------------------------

section "3. CRUD completo de Author"
call POST /authors "" "{\"name\":\"Autor Teste $RUN_ID\",\"bio\":\"bio\"}"
expect_status 401 "POST /authors sem auth -> 401 (ação de escrita exige login)"

call POST /authors "$ADMIN" "{\"name\":\"Autor Teste $RUN_ID\",\"bio\":\"Biografia de teste\"}"
expect_status 201 "POST /authors como admin -> 201"
AUTHOR_ID=$(jq_get '.id')
expect_eq "$(jq_get '.name')" "Autor Teste $RUN_ID" "autor criado com o nome correto"

call GET "/authors/$AUTHOR_ID"
expect_status 200 "GET /authors/{id}"

call PUT "/authors/$AUTHOR_ID" "$ADMIN" "{\"name\":\"Autor Editado $RUN_ID\",\"bio\":\"Nova bio\"}"
expect_status 200 "PUT /authors/{id} como admin -> 200"
expect_eq "$(jq_get '.name')" "Autor Editado $RUN_ID" "nome do autor foi atualizado"

# --- 4. CRUD completo: Category -------------------------------------------

section "4. CRUD de Category"
call POST /categories "$ADMIN" "{\"name\":\"Categoria Teste $RUN_ID\"}"
expect_status 201 "POST /categories como admin -> 201"
CATEGORY_ID=$(jq_get '.id')

call POST /categories "$ADMIN" "{\"name\":\"Categoria Teste $RUN_ID\"}"
expect_status 409 "POST /categories duplicada -> 409"

# --- 5. CRUD completo: Book (com validação e autorização) ----------------

section "5. CRUD completo de Book"
call POST /books "$ADMIN" "{\"title\":\"Livro Teste $RUN_ID\",\"isbn\":\"isbn-$RUN_ID\",\"price\":49.90,\"publishedYear\":2020,\"stock\":10,\"authorId\":$AUTHOR_ID}"
expect_status 201 "POST /books como admin -> 201"
BOOK_ID=$(jq_get '.id')
expect_eq "$(jq_get '.stock')" "10" "livro criado com estoque informado"

call POST /books "$ADMIN" "{\"isbn\":\"sem-titulo\",\"price\":1,\"publishedYear\":2000,\"stock\":1,\"authorId\":$AUTHOR_ID}"
expect_status 422 "POST /books sem título -> 422 (erro de validação)"

call GET "/books/$BOOK_ID"
expect_status 200 "GET /books/{id}"

call PUT "/books/$BOOK_ID" "$ADMIN" "{\"title\":\"Livro Editado $RUN_ID\",\"isbn\":\"isbn-$RUN_ID\",\"price\":59.90,\"publishedYear\":2021,\"stock\":10,\"authorId\":$AUTHOR_ID}"
expect_status 200 "PUT /books/{id} como admin -> 200"
expect_eq "$(jq_get '.price')" "59.90" "preço do livro foi atualizado"

# cliente comum não pode escrever no catálogo
CUSTOMER_EMAIL="customer-$RUN_ID@example.com"
CUSTOMER="$CUSTOMER_EMAIL:secret123"
call POST /users "" "{\"email\":\"$CUSTOMER_EMAIL\",\"password\":\"secret123\",\"name\":\"Cliente Teste\"}"
expect_status 201 "POST /users (cadastro de cliente) -> 201"

call POST /books "$CUSTOMER" "{\"title\":\"Hack\",\"isbn\":\"x-$RUN_ID\",\"price\":1,\"publishedYear\":2000,\"stock\":1,\"authorId\":$AUTHOR_ID}"
expect_status 403 "POST /books como cliente comum -> 403 (precisa ser ADMIN)"

# --- 6. filtro e ordenação (query params) ---------------------------------

section "6. Filtro e ordenação de Books via query params"
call GET "/books?title=$(urlencode "Livro Editado $RUN_ID")"
expect_status 200 "GET /books?title=... -> 200"
expect_eq "$(jq_get 'length')" "1" "filtro por título encontra exatamente o livro criado"

call GET "/books?authorId=$AUTHOR_ID&sortBy=price&sortDir=DESC"
expect_status 200 "GET /books?authorId=&sortBy=&sortDir= -> 200"

call GET "/books?minPrice=1000000"
expect_status 200 "GET /books?minPrice= muito alto -> 200 vazio"
expect_eq "$(jq_get 'length')" "0" "filtro de preço elimina todos os resultados"

call GET "/books?sortBy=campo-invalido"
expect_status 400 "GET /books?sortBy=inválido -> 400"

call GET "/books?sortDir=lateral"
expect_status 400 "GET /books?sortDir=inválido -> 400"

# --- 7. associação Book <-> Category ---------------------------------------

section "7. Associação entre Book e Category"
call POST "/books/$BOOK_ID/categories/$CATEGORY_ID" "" ""
expect_status 401 "POST associação sem auth -> 401"

call POST "/books/$BOOK_ID/categories/$CATEGORY_ID" "$ADMIN" ""
expect_status 201 "POST /books/{id}/categories/{id} como admin -> 201"

call GET "/books/$BOOK_ID"
expect_eq "$(jq_get '.categories | length')" "1" "livro agora tem 1 categoria associada"

call DELETE "/books/$BOOK_ID/categories/$CATEGORY_ID" "$ADMIN"
expect_status 204 "DELETE /books/{id}/categories/{id} como admin -> 204"

call GET "/books/$BOOK_ID"
expect_eq "$(jq_get '.categories | length')" "0" "categoria foi desassociada do livro"

# --- 8. exclusão bloqueada por dependências (409) ---------------------------

section "8. Exclusão bloqueada quando há dependências"
call DELETE "/authors/$AUTHOR_ID" "$ADMIN"
expect_status 409 "DELETE /authors/{id} com livro associado -> 409"

call DELETE "/books/$BOOK_ID" "$ADMIN"
expect_status 204 "DELETE /books/{id} como admin -> 204 (removendo dependência)"

call DELETE "/authors/$AUTHOR_ID" "$ADMIN"
expect_status 204 "DELETE /authors/{id} agora sem livros -> 204"

call DELETE "/categories/$CATEGORY_ID" "$ADMIN"
expect_status 204 "DELETE /categories/{id} sem uso -> 204"

# --- 9. usuários e papéis (grant/revoke) ------------------------------------

section "9. Usuários e papéis (roles)"
call POST /users "" "{\"email\":\"$CUSTOMER_EMAIL\",\"password\":\"secret123\",\"name\":\"Cliente Teste\"}"
expect_status 409 "POST /users com email já cadastrado -> 409"

call GET /users
CUSTOMER_ID=$(jq_get ".[] | select(.email==\"$CUSTOMER_EMAIL\") | .id")

call PUT "/users/$CUSTOMER_ID/roles/CUSTOMER" "$CUSTOMER"
expect_status 403 "PUT roles como o próprio usuário (não admin) -> 403"

call PUT "/users/$CUSTOMER_ID/roles/CUSTOMER" "$ADMIN"
expect_status 200 "PUT /users/{id}/roles/CUSTOMER como admin -> 200"

call GET "/users/$CUSTOMER_ID"
expect_eq "$(jq_get '.roles | contains(["CUSTOMER"]) | tostring')" "true" "usuário recebeu o papel CUSTOMER"

call DELETE "/users/$CUSTOMER_ID/roles/CUSTOMER" "$ADMIN"
expect_status 204 "DELETE /users/{id}/roles/CUSTOMER como admin -> 204"

call PATCH "/users/$CUSTOMER_ID" "$CUSTOMER" "{\"name\":\"Cliente Renomeado\"}"
expect_status 200 "PATCH /users/{id} pelo próprio usuário -> 200"

# --- 10. fluxo completo de pedidos (many-to-many com tabela no meio) -------

section "10. Fluxo de pedidos (Order / OrderItem)"

# autor, categoria e livro dedicados a este bloco de teste
call POST /authors "$ADMIN" "{\"name\":\"Autor Pedido $RUN_ID\"}"
ORDER_AUTHOR_ID=$(jq_get '.id')
call POST /books "$ADMIN" "{\"title\":\"Livro Pedido $RUN_ID\",\"isbn\":\"order-$RUN_ID\",\"price\":20.00,\"publishedYear\":2022,\"stock\":5,\"authorId\":$ORDER_AUTHOR_ID}"
ORDER_BOOK_ID=$(jq_get '.id')

call POST /orders "" ""
expect_status 401 "POST /orders sem auth -> 401"

call POST /orders "$CUSTOMER" ""
expect_status 201 "POST /orders autenticado -> 201"
ORDER_ID=$(jq_get '.id')
expect_eq "$(jq_get '.status')" "OPEN" "pedido criado com status OPEN"

call POST "/orders/$ORDER_ID/items" "$CUSTOMER" "{\"bookId\":$ORDER_BOOK_ID,\"quantity\":2}"
expect_status 201 "POST /orders/{id}/items -> 201"
ITEM_ID=$(jq_get '.items[0].id')
expect_eq "$(jq_get '.total')" "40.00" "total do pedido = 2 x 20.00"

call GET "/books/$ORDER_BOOK_ID"
expect_eq "$(jq_get '.stock')" "3" "estoque do livro caiu de 5 para 3 após a compra"

call POST "/orders/$ORDER_ID/items" "$CUSTOMER" "{\"bookId\":$ORDER_BOOK_ID,\"quantity\":9999}"
expect_status 409 "POST item com quantidade maior que o estoque -> 409"

OTHER_EMAIL="other-$RUN_ID@example.com"
OTHER="$OTHER_EMAIL:secret123"
call POST /users "" "{\"email\":\"$OTHER_EMAIL\",\"password\":\"secret123\",\"name\":\"Outro Usuário\"}"

call GET "/orders/$ORDER_ID" "$OTHER"
expect_status 403 "GET pedido de outro usuário -> 403"

call DELETE "/orders/$ORDER_ID/items/$ITEM_ID" "$OTHER"
expect_status 403 "DELETE item do pedido de outro usuário -> 403"

call GET "/orders/$ORDER_ID" "$ADMIN"
expect_status 200 "GET pedido de outro usuário como ADMIN -> 200 (admin vê tudo)"

call GET /orders "$CUSTOMER"
expect_status 200 "GET /orders (lista os próprios pedidos)"

call DELETE "/orders/$ORDER_ID/items/$ITEM_ID" "$CUSTOMER"
expect_status 204 "DELETE /orders/{id}/items/{itemId} pelo dono -> 204"

call GET "/books/$ORDER_BOOK_ID"
expect_eq "$(jq_get '.stock')" "5" "estoque do livro foi restaurado para 5"

call POST "/orders/$ORDER_ID/items" "$CUSTOMER" "{\"bookId\":$ORDER_BOOK_ID,\"quantity\":1}"
call DELETE "/orders/$ORDER_ID" "$CUSTOMER"
expect_status 200 "DELETE /orders/{id} (cancelamento) pelo dono -> 200"
expect_eq "$(jq_get '.status')" "CANCELED" "pedido ficou com status CANCELED"

call GET "/books/$ORDER_BOOK_ID"
expect_eq "$(jq_get '.stock')" "5" "estoque voltou ao cancelar o pedido"

# --- 11. recursos inexistentes (404) ---------------------------------------

section "11. 404 para recursos inexistentes"
call GET /books/999999
expect_status 404 "GET /books/{id-inexistente} -> 404"
call GET /authors/999999
expect_status 404 "GET /authors/{id-inexistente} -> 404"
call GET /orders/999999 "$ADMIN"
expect_status 404 "GET /orders/{id-inexistente} -> 404"

# --- resumo ------------------------------------------------------------

echo
echo "================================"
echo " Resultado: $PASS passaram, $FAIL falharam"
echo "================================"
[ "$FAIL" -eq 0 ]
