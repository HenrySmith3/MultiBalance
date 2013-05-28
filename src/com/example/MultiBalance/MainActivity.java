package com.example.MultiBalance;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Display;
import android.view.KeyEvent;
import org.anddev.andengine.audio.music.Music;
import org.anddev.andengine.audio.music.MusicFactory;
import org.anddev.andengine.audio.sound.Sound;
import org.anddev.andengine.audio.sound.SoundFactory;
import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.engine.handler.IUpdateHandler;
import org.anddev.andengine.engine.handler.timer.ITimerCallback;
import org.anddev.andengine.engine.handler.timer.TimerHandler;
import org.anddev.andengine.engine.options.EngineOptions;
import org.anddev.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.anddev.andengine.entity.modifier.MoveModifier;
import org.anddev.andengine.entity.modifier.MoveXModifier;
import org.anddev.andengine.entity.scene.CameraScene;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.scene.background.ColorBackground;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.entity.text.ChangeableText;
import org.anddev.andengine.entity.util.FPSLogger;
import org.anddev.andengine.input.touch.TouchEvent;
import org.anddev.andengine.opengl.font.Font;
import org.anddev.andengine.opengl.texture.TextureOptions;
import org.anddev.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.anddev.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.ui.activity.BaseGameActivity;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;

public class MainActivity extends BaseGameActivity implements Scene.IOnSceneTouchListener {

    private Camera mCamera;
    private Scene mMainScene;

    private BitmapTextureAtlas mBitmapTextureAtlas;
    private TextureRegion mPlayerTextureRegion;
    private Sprite player;

    private Sound shootingSound;
    private Music backgroundMusic;

    private TextureRegion mTargetTextureRegion;
    private LinkedList targetLL;
    private LinkedList TargetsToBeAdded;

    private LinkedList projectileLL;
    private LinkedList projectilesToBeAdded;
    private TextureRegion mProjectileTextureRegion;
    private TextureRegion mPausedTextureRegion;
    private CameraScene mPauseScene;
    private android.widget.Toast Toast;

    private CameraScene mResultScene;
    private boolean runningFlag = false;
    private boolean pauseFlag = false;
    private BitmapTextureAtlas mFontTexture;
    private Font mFont;
    private ChangeableText score;
    private int hitCount;
    private final int maxScore = 10;
    private Sprite winSprite;
    private Sprite failSprite;
    private TextureRegion mWinTextureRegion;
    private TextureRegion mFailTextureRegion;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.main);
    }

    @Override
    public Engine onLoadEngine() {

        final Display display = getWindowManager().getDefaultDisplay();
        int cameraWidth = display.getWidth();
        int cameraHeight = display.getHeight();

        mCamera = new Camera(0, 0, cameraWidth, cameraHeight);

        return new Engine(new EngineOptions(true, EngineOptions.ScreenOrientation.LANDSCAPE,
                new RatioResolutionPolicy(cameraWidth, cameraHeight), mCamera)
                .setNeedsMusic(true).setNeedsSound(true));
    }

    @Override
    public void onLoadResources() {
        mBitmapTextureAtlas = new BitmapTextureAtlas(512, 512,
                TextureOptions.BILINEAR_PREMULTIPLYALPHA);

        BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("gfx/");

        mTargetTextureRegion = BitmapTextureAtlasTextureRegionFactory
                .createFromAsset(this.mBitmapTextureAtlas, this, "Target.png",
                        128, 0);

        mPlayerTextureRegion = BitmapTextureAtlasTextureRegionFactory
                .createFromAsset(this.mBitmapTextureAtlas, this, "Player.png",
                        0, 0);
        mEngine.getTextureManager().loadTexture(mBitmapTextureAtlas);

        mProjectileTextureRegion = BitmapTextureAtlasTextureRegionFactory
                .createFromAsset(this.mBitmapTextureAtlas, this,
                        "Projectile.png", 64, 0);

        mPausedTextureRegion = BitmapTextureAtlasTextureRegionFactory
                .createFromAsset(this.mBitmapTextureAtlas, this, "paused.png",
                        0, 64);

        mWinTextureRegion = BitmapTextureAtlasTextureRegionFactory
                .createFromAsset(this.mBitmapTextureAtlas, this, "win.png", 0,
                        128);
        mFailTextureRegion = BitmapTextureAtlasTextureRegionFactory
                .createFromAsset(this.mBitmapTextureAtlas, this, "fail.png", 0,
                        256);

        mEngine.getTextureManager().loadTexture(mBitmapTextureAtlas);
        mFontTexture = new BitmapTextureAtlas(256, 256,
                TextureOptions.BILINEAR_PREMULTIPLYALPHA);
        mFont = new Font(mFontTexture, Typeface.create(Typeface.DEFAULT,
                Typeface.BOLD), 40, true, Color.BLACK);
        mEngine.getTextureManager().loadTexture(mFontTexture);
        mEngine.getFontManager().loadFont(mFont);

        SoundFactory.setAssetBasePath("mfx/");
        try {
            shootingSound = SoundFactory.createSoundFromAsset(mEngine
                    .getSoundManager(), this, "pew_pew_lei.wav");
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        MusicFactory.setAssetBasePath("mfx/");

        try {
            backgroundMusic = MusicFactory.createMusicFromAsset(mEngine
                    .getMusicManager(), this, "background_music_aac.wav");
            backgroundMusic.setLooping(true);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Scene onLoadScene() {
        mEngine.registerUpdateHandler(new FPSLogger());

        final int PlayerX = this.mPlayerTextureRegion.getWidth() / 2;
        final int PlayerY = (int) ((mCamera.getHeight() - mPlayerTextureRegion
                .getHeight()) / 2);

        player = new Sprite(PlayerX, PlayerY, mPlayerTextureRegion);
        player.setScale(2);

        mMainScene = new Scene();
        mMainScene
                .setBackground(new ColorBackground(0.09804f, 0.6274f, 0.8784f));

        mMainScene.attachChild(player);
        targetLL = new LinkedList();
        TargetsToBeAdded = new LinkedList();

        mMainScene.registerUpdateHandler(detect);

        projectileLL = new LinkedList();
        projectilesToBeAdded = new LinkedList();

        mMainScene.setOnSceneTouchListener(this);

        backgroundMusic.play();

        mPauseScene = new CameraScene(mCamera);
        final int x = (int) (mCamera.getWidth() / 2 - mPausedTextureRegion
                .getWidth() / 2);
        final int y = (int) (mCamera.getHeight() / 2 - mPausedTextureRegion
                .getHeight() / 2);
        final Sprite pausedSprite = new Sprite(x, y, mPausedTextureRegion);
        mPauseScene.attachChild(pausedSprite);
        mPauseScene.setBackgroundEnabled(false);

        mResultScene = new CameraScene(mCamera);
        winSprite = new Sprite(x, y, mWinTextureRegion);
        failSprite = new Sprite(x, y, mFailTextureRegion);
        mResultScene.attachChild(winSprite);
        mResultScene.attachChild(failSprite);
        mResultScene.setBackgroundEnabled(false);

        winSprite.setVisible(false);
        failSprite.setVisible(false);
        score = new ChangeableText(0, 0, mFont, String.valueOf(maxScore));
        score.setPosition(mCamera.getWidth() - score.getWidth() - 5, 5);

        return mMainScene;
    }

    @Override
    public void onLoadComplete() {
        createSpriteSpawnTimeHandler();
    }

    public void addTarget() {
        Random rand = new Random();

        int x = (int) mCamera.getWidth() + mTargetTextureRegion.getWidth();
        int minY = mTargetTextureRegion.getHeight();
        int maxY = (int) (mCamera.getHeight() - mTargetTextureRegion
                .getHeight());
        int rangeY = maxY - minY;
        int y = rand.nextInt(rangeY) + minY;

        Sprite target = new Sprite(x, y, mTargetTextureRegion.deepCopy());
        mMainScene.attachChild(target);

        int minDuration = 2;
        int maxDuration = 4;
        int rangeDuration = maxDuration - minDuration;
        int actualDuration = rand.nextInt(rangeDuration) + minDuration;

        MoveXModifier mod = new MoveXModifier(actualDuration, target.getX(),
                -target.getWidth());
        target.registerEntityModifier(mod.deepCopy());

        TargetsToBeAdded.add(target);

    }

    private void shootProjectile(final float pX, final float pY) {

        int offX = (int) (pX - player.getX());
        int offY = (int) (pY - player.getY());
        if (offX <= 0)
            return;

        final Sprite projectile;
        projectile = new Sprite(player.getX(), player.getY(),
                mProjectileTextureRegion.deepCopy());
        mMainScene.attachChild(projectile, 1);

        int realX = (int) (mCamera.getWidth() + projectile.getWidth() / 2.0f);
        float ratio = (float) offY / (float) offX;
        int realY = (int) ((realX * ratio) + projectile.getY());

        int offRealX = (int) (realX - projectile.getX());
        int offRealY = (int) (realY - projectile.getY());
        float length = (float) Math.sqrt((offRealX * offRealX)
                + (offRealY * offRealY));
        float velocity = 480.0f / 1.0f; // 480 pixels / 1 sec
        float realMoveDuration = length / velocity;

        MoveModifier mod = new MoveModifier(realMoveDuration,
                projectile.getX(), realX, projectile.getY(), realY);
        projectile.registerEntityModifier(mod.deepCopy());

        projectilesToBeAdded.add(projectile);

        shootingSound.play();
    }

    private void createSpriteSpawnTimeHandler() {
        TimerHandler spriteTimerHandler;
        float mEffectSpawnDelay = 1f;

        spriteTimerHandler = new TimerHandler(mEffectSpawnDelay, true,
                new ITimerCallback() {

                    @Override
                    public void onTimePassed(TimerHandler pTimerHandler) {
                        addTarget();
                    }
                });

        getEngine().registerUpdateHandler(spriteTimerHandler);
    }

    public void removeSprite(final Sprite _sprite, Iterator it) {
        runOnUpdateThread(new Runnable() {

            @Override
            public void run() {
                mMainScene.detachChild(_sprite);
            }
        });
        it.remove();
    }

    IUpdateHandler detect = new IUpdateHandler() {
        @Override
        public void reset() {
        }

        @Override
        public void onUpdate(float pSecondsElapsed) {

            Iterator<Sprite> targets = targetLL.iterator();
            Sprite _target;
            boolean hit = false;

            while (targets.hasNext()) {
                _target = targets.next();

                if (_target.getX() <= -_target.getWidth()) {
                    removeSprite(_target, targets);
                    fail();
                    break;
                }
                Iterator<Sprite> projectiles = projectileLL.iterator();
                Sprite _projectile;
                while (projectiles.hasNext()) {
                    _projectile = projectiles.next();

                    if (_projectile.getX() >= mCamera.getWidth()
                            || _projectile.getY() >= mCamera.getHeight()
                            + _projectile.getHeight()
                            || _projectile.getY() <= -_projectile.getHeight()) {
                        removeSprite(_projectile, projectiles);
                        continue;
                    }

                    if (_target.collidesWith(_projectile)) {
                        removeSprite(_projectile, projectiles);
                        hit = true;
                        break;
                    }
                }

                if (hit) {
                    removeSprite(_target, targets);
                    hit = false;
                    hitCount++;
                    score.setText(String.valueOf(hitCount));
                }

                if (hitCount >= maxScore) {
                    win();
                }

            }
            projectileLL.addAll(projectilesToBeAdded);
            projectilesToBeAdded.clear();

            targetLL.addAll(TargetsToBeAdded);
            TargetsToBeAdded.clear();
        }
    };

    @Override
    public boolean onSceneTouchEvent(Scene pScene, TouchEvent pSceneTouchEvent) {

        if (pSceneTouchEvent.getAction() == TouchEvent.ACTION_DOWN) {
            final float touchX = pSceneTouchEvent.getX();
            final float touchY = pSceneTouchEvent.getY();
            shootProjectile(touchX, touchY);
            return true;
        }
        return false;
    }

    @Override
    public boolean onKeyDown(final int pKeyCode, final KeyEvent pEvent) {
        if (pKeyCode == KeyEvent.KEYCODE_MENU
                && pEvent.getAction() == KeyEvent.ACTION_DOWN) {
            if (mEngine.isRunning() && backgroundMusic.isPlaying()) {
                pauseMusic();
                pauseFlag = true;
                pauseGame();
                Toast.makeText(this, "Menu button to resume",
                        Toast.LENGTH_SHORT).show();
            } else {
                if (!backgroundMusic.isPlaying()) {
                    unPauseGame();
                    pauseFlag = false;
                    resumeMusic();
                    mEngine.start();
                }
                return true;
            }
        } else if (pKeyCode == KeyEvent.KEYCODE_BACK
                && pEvent.getAction() == KeyEvent.ACTION_DOWN) {

            if (!mEngine.isRunning() && backgroundMusic.isPlaying()) {
                mMainScene.clearChildScene();
                mEngine.start();
                restart();
                return true;
            }
            return super.onKeyDown(pKeyCode, pEvent);
        }
        return super.onKeyDown(pKeyCode, pEvent);
    }

    public void pauseGame() {
        mMainScene.setChildScene(mPauseScene, false, true, true);
        mEngine.stop();
    }

    public void unPauseGame() {
        mMainScene.clearChildScene();
        mEngine.start();
    }

    public void pauseMusic() {
        if (runningFlag)
            if (backgroundMusic.isPlaying())
                backgroundMusic.pause();
    }

    public void resumeMusic() {
        if (runningFlag)
            if (!backgroundMusic.isPlaying())
                backgroundMusic.resume();
    }

    public void restart() {

        runOnUpdateThread(new Runnable() {

            @Override
            public void run() {
                mMainScene.detachChildren();
                mMainScene.attachChild(player, 0);
                mMainScene.attachChild(score);
            }
        });

        hitCount = 0;
        score.setText(String.valueOf(hitCount));
        projectileLL.clear();
        projectilesToBeAdded.clear();
        TargetsToBeAdded.clear();
        targetLL.clear();
    }

    public void fail() {
        if (mEngine.isRunning()) {
            winSprite.setVisible(false);
            failSprite.setVisible(true);
            mMainScene.setChildScene(mResultScene, false, true, true);
            mEngine.stop();
        }
    }

    public void win() {
        if (mEngine.isRunning()) {
            failSprite.setVisible(false);
            winSprite.setVisible(true);
            mMainScene.setChildScene(mResultScene, false, true, true);
            mEngine.stop();
        }
    }

    @Override
    public void onResumeGame() {
        super.onResumeGame();
        if (runningFlag) {
            if (pauseFlag) {
                pauseFlag = false;
                Toast.makeText(this, "Menu button to resume",
                        Toast.LENGTH_SHORT).show();
            } else {
                resumeMusic();
                mEngine.stop();
            }
        } else {
            runningFlag = true;
        }
    }

    @Override
    protected void onPause() {
        if (runningFlag) {
            pauseMusic();
            if (mEngine.isRunning()) {
                pauseGame();
                pauseFlag = true;
            }
        }
        super.onPause();
    }

}
