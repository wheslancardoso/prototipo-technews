# **Blueprint do Projeto TechNews: Do Conceito à Execução**

Este documento é o plano de arquitetura e execução para a construção do protótipo do sistema TechNews. Ele servirá como guia para o desenvolvimento, seja ele assistido por IA ou manual.

## **1\. Arquitetura da Solução**

A aplicação será um **monolito modular** construído com Spring Boot. A arquitetura de alto nível é a seguinte:

\+----------------+      \+--------------------------+      \+-------------------+  
|                |      |                          |      |                   |  
|   Navegador    |-----\>|   Aplicação Spring Boot  |-----\>|   Banco de Dados  |  
| (Usuário/Admin)|      |        (TechNews)        |      |    (PostgreSQL)   |  
|                |      |                          |      |                   |  
\+----------------+      \+-------------+------------+      \+-------------------+  
                                      |  
                                      |  
                      \+---------------+---------------+  
                      |                               |  
              \+-------V-------+               \+-------V-------+  
              |               |               |               |  
              | API da GNews  |               | API do Mailgun|  
              | (Notícias)     |               | (E-mails)     |  
              |               |               |               |  
              \+---------------+               \+---------------+

## **2\. Fluxos Principais**

Descrevemos aqui os três processos centrais do sistema.

### **Fluxo 1: Inscrição de um Novo Usuário**

1. **Usuário** acessa a página inicial (landing page).  
2. Preenche o formulário com **nome** e **e-mail**.  
3. Clica no botão **"Inscrever-se"**.  
4. O **Controller** recebe a requisição.  
5. O **Controller** chama o SubscriberService.  
6. O SubscriberService valida os dados (ex: e-mail em formato válido) e verifica se o e-mail já existe no banco de dados.  
7. Se for válido e novo, o SubscriberService cria um novo objeto Subscriber.  
8. O SubscriberRepository salva o objeto no banco de dados.  
9. O **Controller** redireciona o usuário para uma página de **sucesso**.

### **Fluxo 2: Curadoria de Conteúdo pelo Administrador**

1. **Admin** acessa a URL de login (/login).  
2. Insere usuário e senha.  
3. O **Spring Security** autentica o admin e o redireciona para o dashboard (/admin/dashboard).  
4. O AdminController chama o NewsService para buscar todas as notícias com status PENDENTE\_REVISAO.  
5. O NewsService busca os dados no banco através do NewsArticleRepository.  
6. A página do dashboard é renderizada, mostrando a lista de notícias pendentes.  
7. **Admin** clica em "Aprovar" ou "Rejeitar" para uma notícia.  
8. Uma requisição é enviada para o AdminController (ex: /admin/news/approve/{id}).  
9. O AdminController chama o NewsService para atualizar o status daquela notícia no banco de dados.  
10. A página é recarregada, mostrando a lista atualizada.

### **Fluxo 3: Processo Automático de Envio da Newsletter**

1. O **Scheduler** (@Scheduled) é ativado no horário programado (ex: todo dia às 08:00).  
2. O NewsletterScheduler chama o NewsService para buscar as notícias com status APROVADO.  
3. Se não houver notícias aprovadas, o processo termina.  
4. Se houver, o NewsletterScheduler chama o SubscriberService para obter a lista de todos os assinantes ativos.  
5. Para cada assinante na lista:  
   a. O NewsletterScheduler usa o Thymeleaf para renderizar o template do e-mail com as notícias, gerando uma string HTML.  
   b. O NewsletterScheduler chama o EmailService, passando o e-mail do assinante e o HTML da newsletter.  
   c. O EmailService faz a chamada para a API do Mailgun para enviar o e-mail.  
6. Após o loop de envio terminar, o NewsletterScheduler chama o NewsService para atualizar o status das notícias enviadas para ENVIADO.

## **3\. Modelo do Banco de Dados**

Teremos 3 tabelas principais.

Tabela: subscribers  
| Coluna | Tipo | Descrição |  
| :--- | :--- | :--- |  
| id | BIGINT (PK) | Identificador único do assinante. |  
| nome | VARCHAR(255) | Nome do assinante. |  
| email | VARCHAR(255) | E-mail (único). |  
| ativo| BOOLEAN | Se o assinante está ativo (padrão: true). |  
| data\_inscricao | TIMESTAMP | Data e hora da inscrição. |  
Tabela: trusted\_sources  
| Coluna | Tipo | Descrição |  
| :--- | :--- | :--- |  
| id | BIGINT (PK) | Identificador único da fonte. |  
| domain\_name | VARCHAR(255) | Domínio da fonte confiável (ex: "tecmundo.com.br"). |  
Tabela: news\_articles  
| Coluna | Tipo | Descrição |  
| :--- | :--- | :--- |  
| id | BIGINT (PK) | Identificador único da notícia. |  
| title | VARCHAR(512) | Título da notícia. |  
| description| VARCHAR(2048)| Descrição curta da notícia. |  
| url | VARCHAR(512) | URL original da notícia (único). |  
| image\_url | VARCHAR(512) | URL da imagem de capa da notícia. |  
| source\_domain| VARCHAR(255)| Domínio da fonte (ex: "canaltech.com.br"). |  
| published\_at| TIMESTAMP | Data e hora da publicação original. |  
| status | VARCHAR(50) | Status da curadoria (PENDENTE\_REVISAO, APROVADO, REJEITADO, ENVIADO). |

## **4\. Estrutura de Componentes (Código)**

A estrutura de pacotes seguirá o padrão Spring Boot.

* **br.com.technews.model**  
  * Subscriber.java (Entidade)  
  * TrustedSource.java (Entidade)  
  * NewsArticle.java (Entidade)  
  * ArticleStatus.java (Enum)  
* **br.com.technews.repository**  
  * SubscriberRepository.java (Interface JpaRepository)  
  * TrustedSourceRepository.java (Interface JpaRepository)  
  * NewsArticleRepository.java (Interface JpaRepository)  
* **br.com.technews.service**  
  * SubscriberService.java: Lógica de negócio para assinantes.  
  * NewsService.java: Lógica para buscar, filtrar e gerenciar notícias.  
  * EmailService.java: Lógica para se comunicar com a API do Mailgun.  
* **br.com.technews.controller**  
  * PageController.java: Controla as páginas públicas (home, sucesso, erro).  
  * AdminController.java: Controla o dashboard de administração.  
* **br.com.technews.scheduler**  
  * NewsletterScheduler.java: Contém o método @Scheduled para o envio diário.  
* **br.com.technews.config**  
  * SecurityConfig.java: Configuração do Spring Security para o login do admin.

## **5\. Design das Interfaces (Wireframes)**

#### **a) Página de Inscrição (index.html)**

* **Título Principal:** "TechNews: Sua dose diária de tecnologia."  
* **Subtítulo:** "Receba as notícias mais importantes do mundo da tecnologia, selecionadas por nossa equipe, diretamente no seu e-mail."  
* **Formulário de Inscrição:**  
  * Campo de Texto: Nome  
  * Campo de Texto: E-mail  
  * Botão: Inscrever-se Gratuitamente

#### **b) Dashboard do Admin (admin-dashboard.html)**

* **Título:** "Painel de Curadoria de Notícias"  
* **Tabela de Notícias Pendentes:**  
  * Colunas: Título, Fonte, Data, Ações  
  * Para cada notícia na lista:  
    * O título é um link para a matéria original.  
    * A coluna "Ações" contém dois botões: Aprovar (verde) e Rejeitar (vermelho).

#### **c) Template do E-mail da Newsletter (newsletter-template.html)**

* **Cabeçalho:** Logo "TechNews" e a data do dia.  
* **Seção de Notícias:**  
  * Loop sobre as notícias aprovadas (th:each).  
  * Para cada notícia:  
    * Imagem de capa.  
    * Título da notícia (com link para o artigo original).  
    * Fonte (ex: "Via TecMundo").  
    * Descrição curta.  
    * Uma linha divisória.  
* **Rodapé:** "Você recebeu este e-mail porque se inscreveu na TechNews." com um link para Cancelar Inscrição.