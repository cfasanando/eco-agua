(function () {
  'use strict';

  function isRestaurantPublicMenu() {
    return document.body && document.body.classList.contains('restaurant-menu-public');
  }

  function applyFallbacks() {
    if (!isRestaurantPublicMenu()) {
      return;
    }
    var fallback = '/demo/branding/restaurant-lab/product-placeholder.png';
    document.querySelectorAll('img').forEach(function (img) {
      var src = img.getAttribute('src') || '';
      if (!src || src.indexOf('logo') >= 0 || src.indexOf('favicon') >= 0) {
        return;
      }
      img.addEventListener('error', function () {
        if (img.getAttribute('src') !== fallback) {
          img.setAttribute('src', fallback);
          img.classList.add('restaurant-public-image-fallback');
        }
      });
      if (img.complete && img.naturalWidth === 0 && img.getAttribute('src') !== fallback) {
        img.setAttribute('src', fallback);
        img.classList.add('restaurant-public-image-fallback');
      }
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', applyFallbacks);
  } else {
    applyFallbacks();
  }
})();
