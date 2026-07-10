(() => {
    const modal = document.getElementById('gastoDebtSettlementModal');
    if (!modal) return;

    const money = (value) => Number(value || 0).toLocaleString('en-US', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    });
    const setText = (id, value) => {
        const element = document.getElementById(id);
        if (element) element.textContent = value;
    };

    modal.addEventListener('show.bs.modal', (event) => {
        const trigger = event.relatedTarget;
        if (!trigger) return;

        const currency = trigger.dataset.currency || 'PEN';
        const balance = Number(trigger.dataset.balance || 0);
        const payment = Number(trigger.dataset.payment || 0);
        const gap = Math.max(0, Number(trigger.dataset.gap || 0));
        const opportunity = trigger.dataset.opportunity === 'true';
        const bankReference = trigger.dataset.bankReference === 'true';

        setText('gastoDebtSettlementName', trigger.dataset.debtName || 'Deuda');
        setText('gastoDebtSettlementType', trigger.dataset.debtType || 'Deuda');
        setText('gastoDebtSettlementBalance', `${currency} ${money(balance)}`);
        setText('gastoDebtSettlementPayment', `${currency} ${money(payment)}`);

        let result;
        if (balance <= 0) {
            result = 'El capital registrado ya está cancelado.';
        } else if (opportunity) {
            result = 'El pago previsto para este mes alcanza para cancelar el capital registrado de esta deuda.';
        } else {
            result = `Después del pago mensual todavía quedarían ${currency} ${money(gap)} de capital pendiente.`;
        }
        setText('gastoDebtSettlementResult', result);

        const warning = document.getElementById('gastoDebtSettlementBankWarning');
        warning?.classList.toggle('d-none', !bankReference);

        const editLink = document.getElementById('gastoDebtSettlementEditLink');
        if (editLink) {
            editLink.href = trigger.dataset.editUrl || '#';
            editLink.classList.toggle('disabled', !trigger.dataset.editUrl);
            editLink.setAttribute('aria-disabled', trigger.dataset.editUrl ? 'false' : 'true');
        }
    });
})();
