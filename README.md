# java-pix
### en
Java library to build brazil's PIX Copy &amp; Paste.
Useful to build charges. 
### pt-br
Biblioteca java para construir PIX copia e cola.
Útil para criar "cobranças" com dados predefinidos.
# usage
Built with java 17
## import
```xml
<dependency>
    <groupId>com.iskeru</groupId>
    <artifactId>java-pix</artifactId>
    <version>1.1.0</version>
</dependency>
```

Important: Most Brazilian banks require that the transaction id (txid) be present for the copied PIX code to be accepted when pasting in the banking app. Always set a txid in your PixTransaction when generating a Copy & Paste code.

Ex:

```java
PixCopyAndPaste pix = PixCopyAndPaste.builder()
        .to(PixTarget.builder()
                .chavePix("minha-chave")
                .cidade("SAO PAULO")
                .nome("Person Person")
                .build())
        .transfer(PixTransaction.builder()
                .idTransacao("MEU_TXID_123")
                .valorTransacao(new BigDecimal("12.34"))
                .build())
        .build();
```

Atenção (pt-br): A maioria dos bancos exige que o identificador da transação (txid) esteja presente para que o código PIX Copia e Cola seja aceito ao colar no app. Sempre informe um txid em PixTransaction ao gerar o código.

## code

Build simple copia e cola. 

### PIX Key / Chave PIX
```java
PixCopyAndPaste pix = PixCopyAndPaste.builder()
        .to(PixTarget.builder()
                .chavePix("minha-chave")
                .cidade("SAO PAULO")
                .nome("Person Person")
                .build())
        .transfer(PixTransaction.builder().valorTransacao(new BigDecimal("12.34")).build())
        .build();
```
### PIX phone number / telefone
```java
// when key is a phone number, for instance: (11) 98181-1212
PixTarget.builder().chavePix("+5511981811212").build();
```
### PIX E-mail
```java
// when key is a e-mail
PixTarget.builder().chavePix("someone@gmail.com").build();
```