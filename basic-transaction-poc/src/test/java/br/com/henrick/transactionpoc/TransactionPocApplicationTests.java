package br.com.henrick.transactionpoc;

import br.com.henrick.transactionpoc.model.Conta;
import br.com.henrick.transactionpoc.repository.ContaRepository;
import br.com.henrick.transactionpoc.service.TransferenciaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class TransactionPocApplicationTests {

	@Autowired
	private TransferenciaService transferenciaService;

	@Autowired
	private ContaRepository contaRepository;

	@Test
	void deveFazerRollbackQuandoOcorrerExcecao() {
		// Setup das contas no banco
		Conta origem = contaRepository.save(new Conta(null, "Henrick", new BigDecimal("1000.00")));
		Conta destino = contaRepository.save(new Conta(null, "Maria", new BigDecimal("500.00")));

		// Execução: tenta transferir 200 e dispara o erro
		assertThrows(RuntimeException.class, () -> {
			transferenciaService.transferirComErro(origem.getId(), destino.getId(), new BigDecimal("200.00"));
		});

		// Asserção: O saldo da conta de origem DEVE permanecer 1000.00 devido ao Rollback
		Conta origemPosErro = contaRepository.findById(origem.getId()).orElseThrow();
		assertThat(origemPosErro.getSaldo()).isEqualByComparingTo(new BigDecimal("1000.00"));
	}
}