(function () {
  function absoluteUrl(relativeUrl) {
    return new URL(relativeUrl || '/restaurant/menu', window.location.origin).toString();
  }

  function qrUrl(value) {
    return 'https://api.qrserver.com/v1/create-qr-code/?size=220x220&margin=12&data=' + encodeURIComponent(value);
  }

  function setQrImages() {
    document.querySelectorAll('.js-restaurant-qr').forEach(function (image) {
      var link = absoluteUrl(image.getAttribute('data-public-menu-link'));
      image.setAttribute('src', qrUrl(link));
    });
  }

  function setPublicLinks() {
    document.querySelectorAll('.js-restaurant-public-link').forEach(function (input) {
      input.value = absoluteUrl(input.getAttribute('data-public-menu-link'));
    });
  }

  function bindCopyButtons() {
    document.querySelectorAll('.js-restaurant-copy-link').forEach(function (button) {
      button.addEventListener('click', function () {
        var group = button.closest('.input-group');
        var input = group ? group.querySelector('.js-restaurant-public-link') : null;
        if (!input) {
          return;
        }
        navigator.clipboard.writeText(input.value).then(function () {
          var original = button.innerHTML;
          button.innerHTML = '<i class="bi bi-check2"></i>';
          setTimeout(function () {
            button.innerHTML = original;
          }, 1200);
        });
      });
    });
  }

  document.addEventListener('DOMContentLoaded', function () {
    setQrImages();
    setPublicLinks();
    bindCopyButtons();
  });
})();
