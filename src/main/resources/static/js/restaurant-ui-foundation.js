(function () {
  'use strict';

  function isRestaurantAdmin() {
    return window.location && window.location.pathname.indexOf('/admin/restaurant') === 0;
  }

  function addRestaurantUiClass() {
    if (!isRestaurantAdmin()) {
      return;
    }
    document.body.classList.add('restaurant-ui');
    document.documentElement.classList.add('restaurant-ui-root');
  }

  function applyImageFallbacks() {
    if (!isRestaurantAdmin()) {
      return;
    }
    var fallback = '/demo/branding/restaurant-lab/product-placeholder.png';
    document.querySelectorAll('img').forEach(function (img) {
      var current = img.getAttribute('src') || '';
      if (current.indexOf('logo') >= 0 || current.indexOf('favicon') >= 0) {
        return;
      }
      img.addEventListener('error', function () {
        if (img.getAttribute('src') !== fallback) {
          img.setAttribute('src', fallback);
          img.classList.add('restaurant-image-fallback');
        }
      });
      if (img.complete && img.naturalWidth === 0 && img.getAttribute('src') !== fallback) {
        img.setAttribute('src', fallback);
        img.classList.add('restaurant-image-fallback');
      }
    });
  }

  function markDenseCards() {
    if (!isRestaurantAdmin()) {
      return;
    }
    document.querySelectorAll('.card').forEach(function (card) {
      if (card.querySelector('table')) {
        card.classList.add('restaurant-table-card');
      }
      if (card.querySelector('img') && card.querySelector('input[type="number"]')) {
        card.classList.add('restaurant-product-card');
      }
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', function () {
      addRestaurantUiClass();
      applyImageFallbacks();
      markDenseCards();
    });
  } else {
    addRestaurantUiClass();
    applyImageFallbacks();
    markDenseCards();
  }
})();
