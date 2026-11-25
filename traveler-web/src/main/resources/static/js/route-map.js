// Route map of a plan page: numbered markers in visit order joined by a line (Leaflet + OSM).
(function () {
  const container = document.getElementById('map');
  if (!container || typeof L === 'undefined' || !Array.isArray(points) || points.length === 0) return;

  const map = L.map(container, { scrollWheelZoom: false });
  L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 18,
    attribution: '&copy; OpenStreetMap contributors'
  }).addTo(map);

  const escape = (text) => String(text).replace(/[&<>"']/g,
    (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

  const latLngs = points.map((p) => [p.lat, p.lng]);
  points.forEach((p) => {
    const icon = L.divIcon({
      className: '',
      html: '<span class="order" style="box-shadow:0 0 0 3px #fff">' + p.order + '</span>',
      iconSize: [28, 28],
      iconAnchor: [14, 14]
    });
    L.marker([p.lat, p.lng], { icon: icon, title: p.name })
      .bindPopup('<b>' + p.order + '. ' + escape(p.name) + '</b><br>' + escape(p.visit))
      .addTo(map);
  });
  if (latLngs.length > 1) {
    L.polyline(latLngs, { color: '#0d6b6b', weight: 3, opacity: 0.8, dashArray: '6 6' }).addTo(map);
  }
  if (latLngs.length === 1) {
    map.setView(latLngs[0], 12);
  } else {
    map.fitBounds(latLngs, { padding: [36, 36] });
  }
})();
