package tools;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Random;

import javax.imageio.ImageIO;

import org.jdesktop.j3d.examples.sound.PointSoundBehavior;
import org.jdesktop.j3d.examples.sound.audio.JOALMixer;
import org.jogamp.java3d.*;
import org.jogamp.java3d.TransparencyAttributes;
import org.jogamp.java3d.loaders.IncorrectFormatException;
import org.jogamp.java3d.loaders.ParsingErrorException;
import org.jogamp.java3d.loaders.Scene;
import org.jogamp.java3d.loaders.objectfile.ObjectFile;
import org.jogamp.java3d.utils.geometry.Box;
import org.jogamp.java3d.utils.geometry.Primitive;
import org.jogamp.java3d.utils.image.TextureLoader;
import org.jogamp.java3d.utils.universe.SimpleUniverse;
import org.jogamp.java3d.utils.universe.Viewer;
import org.jogamp.vecmath.*;

import ECS.ESystem;
import ECS.Entity;
import entities.Entities;
import entry.Game;
import enums.TankColor;
import enums.TorchDirection;
import networking.User;

public class Util {
	public final static Color3f RED = new Color3f(1.0f, 0.0f, 0.0f);
	public final static Color3f GREEN = new Color3f(0.0f, 1.0f, 0.0f);
	public final static Color3f BLUE = new Color3f(0.0f, 0.0f, 1.0f);
	public final static Color3f YELLOW = new Color3f(1.0f, 1.0f, 0.0f);
	public final static Color3f CYAN = new Color3f(0.0f, 1.0f, 1.0f);
	public final static Color3f ORANGE = new Color3f(1.0f, 0.5f, 0.0f);
	public final static Color3f MAGENTA = new Color3f(1.0f, 0.0f, 1.0f);
	public final static Color3f WHITE = new Color3f(1.0f, 1.0f, 1.0f);
	public final static Color3f GREY = new Color3f(0.35f, 0.35f, 0.35f);
	public final static Color3f BLACK = new Color3f(0.0f, 0.0f, 0.0f);

	public final static BoundingSphere LIGHT_BOUNDS = new BoundingSphere(new Point3d(), 10000.0);

	public static BranchGroup createInvisibleFloor() {
		Appearance ap = Util.createAppearance(new Color3f(0.16f, 0.17f, 0.204f), Util.BLACK, new Color3f(0.2f, 0.2f, 0.2f), 0, null);
        TransparencyAttributes transparency = new TransparencyAttributes(TransparencyAttributes.SCREEN_DOOR, 1f,
				TransparencyAttributes.BLEND_SRC_ALPHA, TransparencyAttributes.BLEND_ONE);
		ap.setTransparencyAttributes(transparency);
        Box box = new Box(100, 0.2f, 100, Primitive.GENERATE_NORMALS, ap);
        TransformGroup floorTg = new TransformGroup();
        floorTg.addChild(box);
        Transform3D transform = new Transform3D();
        transform.setTranslation(new Vector3f(78, 1.13528f - 1.4f, 78));
        Transform3D rotTransform = new Transform3D();
        Transform3D wholeThing = new Transform3D();
        wholeThing.mul(transform, rotTransform);
        
        floorTg.setTransform(wholeThing);
        BranchGroup floorBg = new BranchGroup();
        floorBg.addChild(floorTg);
        return floorBg;
	}
	
	public static PointSound createPointSound(String filepath, float volume, int loop, Point3f position) {
        PointSound ps = new PointSound();
        ps.setCapability(PointSound.ALLOW_ENABLE_WRITE);
        ps.setCapability(PointSound.ALLOW_LOOP_WRITE);
        ps.setEnable(true);
        ps.setInitialGain(volume);
        ps.setLoop(loop);
        ps.setBounds(Util.LIGHT_BOUNDS);

        PointSoundBehavior behavior = new PointSoundBehavior(ps, Util.locateSound(filepath), position);
        behavior.setBounds(Util.LIGHT_BOUNDS);
        return ps;
    }
	
	public static void enableAudio(SimpleUniverse simple_U) {
		JOALMixer mixer = null; // create a joalmixer
        Viewer viewer = simple_U.getViewer();
        viewer.getView().setBackClipDistance(4000.0f);
        if (mixer == null && viewer.getView().getUserHeadToVworldEnable()) {
            mixer = new JOALMixer(viewer.getPhysicalEnvironment());
            if (!mixer.initialize()) {// add audio device
                System.out.println("Open AL failed to init");
                viewer.getPhysicalEnvironment().setAudioDevice(null);
            }
        }
	}

	public static float distance(Vector2f point1, Vector2f point2) {
        float dx = point1.x - point2.x;
        float dy = point1.y - point2.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

	public static boolean isLocalIPAddress(String ipAddress) {
		try {
			return InetAddress.getByName(ipAddress).isSiteLocalAddress();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		return false;
    }
	
	public static void closeAudioDevice(SimpleUniverse su) {
		su.getViewer().getPhysicalEnvironment().getAudioDevice().close();
	}

	public static Entity setupMaze(String filepath, TransformGroup sceneTG, ESystem eSystem) throws Exception {
		BufferedImage img = ImageIO.read(new File(filepath));
		Entity userTank = null;
		float variation = -0.5f;

		for (int y = -10; y < img.getHeight() + 10; y++) {
			for (int x = -10; x < img.getWidth() + 10; x++) {
				if (new Random().nextInt(28) == 1)
					eSystem.addEntity(Entities.createFirefly(new Vector3f(x * 4,
						(y < 0 || x < 0 || x >= img.getWidth() || y >= img.getHeight()) ? 16 : 2 - variation, y * 4), sceneTG));
			}
		}

		for (int y = 0; y < img.getHeight(); y++) {
			for (int x = 0; x < img.getWidth(); x++) {
				int pixel = img.getRGB(x, y);

				int red = (pixel >> 16) & 255;
                int green = (pixel >> 8) & 255;
                int blue = pixel & 255;

				if (red == 0 && green == 0 && blue == 0)
					eSystem.addEntity(Entities.createBlock(sceneTG, new Vector3f(x * 4, 2 - variation, y * 4)));
				
				if(red == 255 && green == 0 && blue == 0 && Game.room == null)
					eSystem.addEntity(userTank = Entities.createUserTank(sceneTG, new Vector3f(x * 4, -1.4f, y * 4), TankColor.RED));
				
				if (red == 255 && green == 106 && blue == 0) {
					eSystem.addEntity(Entities.createBlock(sceneTG, new Vector3f(x * 4, 2 - variation, y * 4)));
					eSystem.addEntity(Entities.createTorch(sceneTG, new Vector3f(x * 4, 2 - variation, y * 4), TorchDirection.FORWARD));
				}

				if (red == 255 && green == 255 && blue == 0) {
					eSystem.addEntity(Entities.createBlock(sceneTG, new Vector3f(x * 4, 2 - variation, y * 4)));
					eSystem.addEntity(Entities.createTorch(sceneTG, new Vector3f(x * 4, 2 - variation, y * 4), TorchDirection.BACK));
				}

				if (red == 0 && green == 0 && blue == 255) {
					eSystem.addEntity(Entities.createBlock(sceneTG, new Vector3f(x * 4, 2 - variation, y * 4)));
					eSystem.addEntity(Entities.createTorch(sceneTG, new Vector3f(x * 4, 2 - variation, y * 4), TorchDirection.RIGHT));
				}

				if (red == 0 && green == 255 && blue == 0) {
					eSystem.addEntity(Entities.createBlock(sceneTG, new Vector3f(x * 4, 2 - variation, y * 4)));
					eSystem.addEntity(Entities.createTorch(sceneTG, new Vector3f(x * 4, 2 - variation, y * 4), TorchDirection.LEFT));
				}

				if (red == 178 && green == 71 && blue == 0) {
					eSystem.addEntity(Entities.createBarrels(sceneTG, new Vector3f(x * 4, -1, y * 4)));
				}

				if (red == 178 && green == 0 && blue == 255)
					eSystem.addEntity(Entities.createPlantPot(sceneTG, new Vector3f(x * 4, -1.5f, y * 4)));

				if (red == 0 && green == 255 && blue == 255) {
					eSystem.addEntity(Entities.createBlock(sceneTG, new Vector3f(x * 4, 2 - variation, y * 4)));
					eSystem.addEntity(Entities.createSkull(sceneTG, new Vector3f(x * 4, 2 - variation, y * 4)));
				}
			}
		}
		
		if(Game.room != null) {
			for(int i = 0; i < Game.room.users.size(); i++) {
				User currentUser = Game.room.users.get(i);
				if(Game.user.equals(currentUser)) {
					eSystem.addEntity(userTank = Entities.createUserTank(sceneTG, Game.user.position, Game.user.tColor));
				} else {
					eSystem.addEntity(Entities.createNetworkTank(sceneTG, currentUser.position, currentUser.username, currentUser.tColor));
				}
			}
		}
		
		return userTank;
	}

	public static URL locateSound(String filepath) {
		URL url = null;
		try {// locate the file
			url = new URL("file", "localhost", filepath);
		} catch (Exception e) {
			System.out.println("Can't open " + filepath);
		}
		return url;
	}

	public static PointSound pointSound(String name) {
		MediaContainer pointMedia = new MediaContainer(locateSound(name));
		pointMedia.setCacheEnable(true); // enable cache
		PointSound pointSound = new PointSound();// create point sound
		pointSound.setSoundData(pointMedia);// set sound file
		pointSound.setEnable(true);// enable sound
		pointSound.setInitialGain(1.0f);// set initial gain
		pointSound.setLoop(-1);// 0: once; -1: loop
		pointSound.setSchedulingBounds(LIGHT_BOUNDS);// set schuduling
		return pointSound;
	}

	public static Appearance createAppearance(Color3f diffuse, Color3f specular, Color3f emmissive, float shininess, TextureData texData) {
		Material mtl = new Material();
		mtl.setCapability(Material.ALLOW_COMPONENT_READ);
		mtl.setCapability(Material.ALLOW_COMPONENT_WRITE);

		mtl.setShininess(shininess);
		mtl.setAmbientColor(BLACK);
		mtl.setDiffuseColor(diffuse);
		mtl.setSpecularColor(specular);
		mtl.setEmissiveColor(emmissive);
		mtl.setLightingEnable(true);

		Appearance appearance = new Appearance();
		appearance.setCapability(Appearance.ALLOW_MATERIAL_READ);
		appearance.setCapability(Appearance.ALLOW_MATERIAL_WRITE);
		appearance.setMaterial(mtl);

		if (texData != null) {
			appearance.setTexture(texData.texture);
			appearance.setTextureAttributes(texData.texAttr);
		}

		return appearance;
	}

	public static float clamp(float value, float min, float max) {
        if (value < min) {
            return min;
        } else if (value > max) {
            return max;
        } else {
            return value;
        }
    }

	public static void addDirectionalLight(BranchGroup lightGroup, Vector3f direction, Color3f color) {
		DirectionalLight directionalLight = new DirectionalLight(color, direction);
		directionalLight.setInfluencingBounds(LIGHT_BOUNDS);
		lightGroup.addChild(directionalLight);
	}

	public static TextureData loadTexture(String filepath) {
		TextureData data = new TextureData();
		
		TextureLoader loader = new TextureLoader(filepath, null);
		data.texture = (Texture2D) loader.getTexture();
		data.texture.setMinFilter(Texture2D.BASE_LEVEL_POINT);
		data.texture.setMagFilter(Texture2D.BASE_LEVEL_POINT);

		data.texAttr = new TextureAttributes();
        data.texAttr.setTextureMode(TextureAttributes.MODULATE);

		return data;
	}

	public static float lerp(float start, float end, float t) {
        return start + t * (end - start);
    }

	public static Color3f mix(Color3f start, Color3f end, float t) {
	    float x = lerp(start.x, end.x, t);
	    float y = lerp(start.y, end.y, t);
	    float z = lerp(start.z, end.z, t);
	    return new Color3f(x, y, z);
	}

	public static Vector3f lerp(Vector3f start, Vector3f end, float t) {
	    float x = lerp(start.x, end.x, t);
	    float y = lerp(start.y, end.y, t);
	    float z = lerp(start.z, end.z, t);
	    return new Vector3f(x, y, z);
	}
	
	public static BranchGroup load3DModel(String filepath) {
		ObjectFile f = new ObjectFile();
		Scene s = null;
        try {
            s = f.load(filepath);
        } catch (FileNotFoundException e) {
            System.err.println(e);
            System.exit(1);
        } catch (ParsingErrorException e) {
            System.err.println(e);
            System.exit(1);
        } catch (IncorrectFormatException e) {
            System.err.println(e);
            System.exit(1);
        }

        return s.getSceneGroup();
	}
}