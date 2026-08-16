(function () {
  'use strict';

  var stage = null;
  var motion = null;
  var kicker = null;
  var title = null;
  var description = null;
  var dismissTimer = null;
  var clearTimer = null;
  var pitchTimer = null;
  var pitchQueue = [];
  var pitchActive = false;
  var playActive = false;
  var fireworksTimer = null;
  var reducedMotion = window.matchMedia
    ? window.matchMedia('(prefers-reduced-motion: reduce)')
    : { matches: false };

  function normalize(value) {
    return String(value || '').trim().toLowerCase();
  }

  function classify(play) {
    var eventType = normalize(play.lastPlayEvent).replace(/-/g, '_');
    var words = normalize((play.lastPlayLabel || '') + ' ' + (play.lastPlayDescription || ''));

    if (eventType.indexOf('home_run') !== -1) {
      return play.lastPlayByRedSox ? 'home-run' : 'generic';
    }
    if (eventType.indexOf('strikeout') !== -1 || words.indexOf('strikeout') !== -1
        || words.indexOf('strikes out') !== -1) return 'strikeout';
    if (eventType.indexOf('walk') !== -1 || words.indexOf('walk') !== -1) return 'walk';
    if (eventType === 'single' || eventType === 'double' || eventType === 'triple') return 'hit';
    if (eventType.indexOf('double_play') !== -1 || /double[ -]?play/.test(words)) return 'double-play';
    if (/pop[ -]?out|pops out/.test(words)) return 'popout';
    if (eventType.indexOf('flyout') !== -1 || eventType.indexOf('lineout') !== -1
        || /fly[ -]?out|flies out|line[ -]?out|lines out/.test(words)) return 'flyout';
    if (/ground[ -]?out|grounds out|grounded into/.test(words)) return 'groundout';
    return 'generic';
  }

  function fallbackLabel(eventType) {
    return String(eventType || 'Play complete')
      .replace(/[_-]+/g, ' ')
      .replace(/\b\w/g, function (letter) { return letter.toUpperCase(); });
  }

  function ensureElements() {
    if (stage) return true;
    stage = document.getElementById('live-play-stage');
    if (!stage) return false;
    motion = stage.querySelector('.live-play-motion');
    kicker = stage.querySelector('.live-play-kicker');
    title = stage.querySelector('.live-play-title');
    description = stage.querySelector('.live-play-description');
    return !!motion && !!kicker && !!title && !!description;
  }

  function clearMotion() {
    if (motion) motion.replaceChildren();
  }

  function addSweeper(kind) {
    clearMotion();
    if (kind === 'strikeout') {
      var fireball = document.createElement('span');
      fireball.className = 'live-play-fireball';
      motion.appendChild(fireball);
      return;
    }
    if (kind === 'groundout' || kind === 'flyout' || kind === 'double-play' || kind === 'hit') {
      var ball = document.createElement('img');
      ball.className = 'live-play-baseball';
      ball.src = stage.getAttribute('data-baseball-src') || '/images/baseball-play.png';
      ball.alt = '';
      motion.appendChild(ball);
      if (kind !== 'hit') {
        var gloveRight = document.createElement('img');
        gloveRight.className = 'live-play-glove is-right';
        gloveRight.src = stage.getAttribute('data-glove-src') || '/images/play-glove.webp';
        gloveRight.alt = '';
        motion.appendChild(gloveRight);
      }
      if (kind === 'double-play') {
        var gloveLeft = document.createElement('img');
        gloveLeft.className = 'live-play-glove is-left';
        gloveLeft.src = stage.getAttribute('data-glove-src') || '/images/play-glove.webp';
        gloveLeft.alt = '';
        motion.appendChild(gloveLeft);
      }
      return;
    }
    if (kind === 'walk') {
      var walker = document.createElement('span');
      walker.className = 'live-play-walker';
      walker.textContent = '\uD83D\uDEB6';
      motion.appendChild(walker);
    }
  }

  function removeFireworks() {
    var old = document.querySelector('.home-run-fireworks');
    if (old) old.remove();
    if (fireworksTimer) {
      window.clearTimeout(fireworksTimer);
      fireworksTimer = null;
    }
  }

  function launchFireworks() {
    removeFireworks();
    if (reducedMotion.matches) return;

    var layer = document.createElement('div');
    layer.className = 'home-run-fireworks';
    layer.setAttribute('aria-hidden', 'true');
    var colors = ['#d7272d', '#ffffff', '#ffd34e', '#ff6b3d'];
    var burstCount = window.innerWidth < 600 ? 3 : 5;
    var sparksPerBurst = window.innerWidth < 600 ? 12 : 16;

    for (var burst = 0; burst < burstCount; burst++) {
      var x = 12 + Math.random() * 76;
      var y = 13 + Math.random() * 42;
      var delay = burst * 0.22;
      var rocket = document.createElement('span');
      rocket.className = 'firework-rocket';
      rocket.style.setProperty('--rocket-x', x + 'vw');
      rocket.style.setProperty('--rocket-rise', '-' + (100 - y) + 'vh');
      rocket.style.setProperty('--firework-delay', delay + 's');
      layer.appendChild(rocket);

      for (var i = 0; i < sparksPerBurst; i++) {
        var angle = (Math.PI * 2 * i / sparksPerBurst) + (Math.random() * 0.24 - 0.12);
        var distance = 45 + Math.random() * 100;
        var drift = Math.cos(angle) * distance;
        var lift = Math.sin(angle) * distance - 28;
        var spark = document.createElement('span');
        spark.className = 'firework-spark';
        spark.style.setProperty('--spark-x', x + 'vw');
        spark.style.setProperty('--spark-y', y + 'vh');
        spark.style.setProperty('--spark-drift', drift + 'px');
        spark.style.setProperty('--spark-drift-end', (drift * 1.45) + 'px');
        spark.style.setProperty('--spark-lift', lift + 'px');
        spark.style.setProperty('--spark-fall', (window.innerHeight * (1 - y / 100) + 90) + 'px');
        spark.style.setProperty('--spark-color', colors[(i + burst) % colors.length]);
        spark.style.setProperty('--firework-delay', (delay + 0.5) + 's');
        spark.style.setProperty('--spark-duration', (2.5 + Math.random() * 1.2) + 's');
        layer.appendChild(spark);
      }
    }

    document.body.appendChild(layer);
    fireworksTimer = window.setTimeout(removeFireworks, 5200);
  }

  function hide(afterHide) {
    if (!stage) return;
    stage.classList.add('is-leaving');
    clearTimer = window.setTimeout(function () {
      stage.className = 'live-play-stage';
      stage.setAttribute('aria-hidden', 'true');
      clearMotion();
      clearTimer = null;
      if (afterHide) afterHide();
    }, 360);
  }

  function resetDescription() {
    description.classList.remove('is-pitch-details');
    description.replaceChildren();
  }

  function showNextPitch() {
    if (pitchActive || playActive || !pitchQueue.length || !ensureElements()) return;
    var pitch = pitchQueue.shift();
    pitchActive = true;

    stage.className = 'live-play-stage';
    stage.setAttribute('aria-hidden', 'false');
    kicker.textContent = pitch.count || 'Pitch';
    title.textContent = pitch.speed || 'Speed unavailable';
    resetDescription();
    description.classList.add('is-pitch-details');

    var type = document.createElement('span');
    type.className = 'live-pitch-type';
    type.textContent = pitch.type || 'Pitch type unavailable';
    var call = document.createElement('strong');
    call.className = 'live-pitch-call';
    call.textContent = pitch.call || 'Pitch';
    description.append(type, call);
    description.hidden = false;
    clearMotion();

    void stage.offsetWidth;
    stage.classList.add('is-active', 'is-pitch');
    pitchTimer = window.setTimeout(function () {
      pitchTimer = null;
      hide(function () {
        pitchActive = false;
        showNextPitch();
      });
    }, 2800);
  }

  function enqueuePitches(pitches) {
    if (!Array.isArray(pitches) || !pitches.length) return;
    pitches.forEach(function (pitch) {
      if (pitch && pitch.id) pitchQueue.push(pitch);
    });
    showNextPitch();
  }

  function interruptPitches() {
    pitchQueue = [];
    pitchActive = false;
    if (pitchTimer) {
      window.clearTimeout(pitchTimer);
      pitchTimer = null;
    }
  }

  function announce(play) {
    if (!play || !ensureElements()) return;
    if (dismissTimer) window.clearTimeout(dismissTimer);
    if (clearTimer) window.clearTimeout(clearTimer);
    interruptPitches();
    playActive = true;

    var kind = classify(play);
    var label = play.lastPlayLabel || fallbackLabel(play.lastPlayEvent);
    var detail = play.lastPlayDescription || play.lastPlayBatter || '';

    stage.className = 'live-play-stage';
    stage.setAttribute('aria-hidden', 'false');
    kicker.textContent = 'Live Play';
    title.textContent = label;
    resetDescription();
    description.textContent = detail;
    description.hidden = !detail;
    addSweeper(kind);

    // Restart the transition even when consecutive at-bats have the same result.
    void stage.offsetWidth;
    stage.classList.add('is-active', 'is-' + kind);

    if (kind === 'home-run') launchFireworks();
    dismissTimer = window.setTimeout(function () {
      dismissTimer = null;
      hide(function () {
        playActive = false;
        showNextPitch();
      });
    }, kind === 'home-run' ? 8200 : 6500);
  }

  window.NumSoxLivePlays = {
    announce: announce,
    enqueuePitches: enqueuePitches,
    classify: classify
  };
})();
