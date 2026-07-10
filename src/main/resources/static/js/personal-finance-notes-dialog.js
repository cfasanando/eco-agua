(() => {
    const modalElement = document.getElementById('gastoNotesModal');
    if (!modalElement || modalElement.dataset.gastoNotesReady === 'true') return;

    modalElement.dataset.gastoNotesReady = 'true';

    const conceptElement = document.getElementById('gastoNotesConcept');
    const sourceElement = document.getElementById('gastoNotesSource');
    const sourceRow = document.getElementById('gastoNotesSourceRow');
    const notesElement = document.getElementById('gastoNotesText');

    const readText = (trigger, selector) => {
        const element = trigger?.querySelector(selector);
        return element?.textContent?.trim() || '';
    };

    const resetDialog = () => {
        if (conceptElement) conceptElement.textContent = '-';
        if (sourceElement) sourceElement.textContent = '-';
        if (sourceRow) sourceRow.hidden = false;
        if (notesElement) notesElement.textContent = '-';
    };

    modalElement.addEventListener('show.bs.modal', (event) => {
        const trigger = event.relatedTarget;
        const concept = readText(trigger, '.js-gasto-note-title');
        const source = readText(trigger, '.js-gasto-note-source');
        const notes = readText(trigger, '.js-gasto-note-content');

        if (conceptElement) conceptElement.textContent = concept || 'Sin concepto';
        if (sourceElement) sourceElement.textContent = source || '-';
        if (sourceRow) sourceRow.hidden = !source;
        if (notesElement) notesElement.textContent = notes || 'Sin observación registrada.';
    });

    modalElement.addEventListener('hidden.bs.modal', resetDialog);
})();
