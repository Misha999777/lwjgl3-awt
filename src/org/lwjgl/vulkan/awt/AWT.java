package org.lwjgl.vulkan.awt;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.jawt.JAWT;
import org.lwjgl.system.jawt.JAWTDrawingSurface;
import org.lwjgl.system.jawt.JAWTDrawingSurfaceInfo;

import java.awt.*;

import static org.lwjgl.system.jawt.JAWTFunctions.*;

/**
 * Platform-independent implementation<sup>&#8224;</sup>.
 * <p>
 * &#8224; The JAWT library is initialized with different versions
 * depending on the platform for some reason.
 *
 * @author SWinxy
 * @author Kai Burjack
 */
public class AWT implements AutoCloseable {

	private final JAWT jawt;
	private final JAWTDrawingSurface drawingSurface;
	private final JAWTDrawingSurfaceInfo drawingSurfaceInfo;

	/**
	 * Initializes native window handlers from the desired AWT component.
	 * The component MUST be a {@link Component}, but should be a canvas
	 * or window for native rendering.
	 *
	 * @param component a component to render onto
	 * @throws AWTException Fails for one of the provided reasons:
	 *                      <ul>
	 *                          <li>if the JAWT library failed to initialize;</li>
	 *                          <li>if the drawing surface could not be retrieved;</li>
	 *                          <li>if JAWT failed to lock the drawing surface;</li>
	 *                          <li>or if JAWT failed to get information about the drawing surface;</li>
	 *                      </ul>
	 */
	public AWT(Component component) throws AWTException {
		try (MemoryStack stack = MemoryStack.stackPush()) {

			jawt = JAWT
					.callocStack(stack)
					.version(JAWT_VERSION_1_7);

			// Initialize JAWT
			if (!JAWT_GetAWT(jawt)) {
				throw new AWTException("Failed to initialize the native JAWT library.");
			}

			// Get the drawing surface from the canvas
			drawingSurface = JAWT_GetDrawingSurface(component, jawt.GetDrawingSurface());
			if (drawingSurface == null) {
				throw new AWTException("Failed to get drawing surface.");
			}

			// Try to lock the surface for native rendering
			int lock = JAWT_DrawingSurface_Lock(drawingSurface, drawingSurface.Lock());
			if ((lock & JAWT_LOCK_ERROR) != 0) {
				JAWT_FreeDrawingSurface(drawingSurface, jawt.FreeDrawingSurface());
				throw new AWTException("Failed to lock the AWT drawing surface.");
			}

			drawingSurfaceInfo = JAWT_DrawingSurface_GetDrawingSurfaceInfo(drawingSurface, drawingSurface.GetDrawingSurfaceInfo());
			if (drawingSurfaceInfo == null) {
				JAWT_DrawingSurface_Unlock(drawingSurface, drawingSurface.Unlock());
				throw new AWTException("Failed to get AWT drawing surface information.");
			}

			long address = drawingSurfaceInfo.platformInfo();

			if (address == MemoryUtil.NULL) {
				throw new AWTException("An unknown error occurred. Failed to retrieve platform-specific information.");
			}
		}
	}

	public static AWT create(Canvas canvas) throws AWTException {
		return new AWT(canvas);
	}

	/**
	 * Returns a pointer to a platform-specific struct with platform-specific information.
	 * <p>
	 * The pointer can be safely cast to a {@link org.lwjgl.system.jawt.JAWTWin32DrawingSurfaceInfo}
	 * or {@link org.lwjgl.system.jawt.JAWTX11DrawingSurfaceInfo} struct, or--if on MacOS--
	 * a pointer to an {@code NSObject}.
	 * <p>
	 * On win32 or X11 platforms, this can easily be created in Java via LWJGL's
	 * {@code #create(long)} method.
	 *
	 * @return pointer to platform-specific data
	 */
	public long getPlatformInfo() {
		return drawingSurfaceInfo.platformInfo();
	}

	public JAWTDrawingSurfaceInfo getDrawingSurfaceInfo() {
		return drawingSurfaceInfo;
	}

	@Override
	public void close() {
		// Free and unlock
		JAWT_DrawingSurface_FreeDrawingSurfaceInfo(drawingSurfaceInfo, drawingSurface.FreeDrawingSurfaceInfo());
		JAWT_DrawingSurface_Unlock(drawingSurface, drawingSurface.Unlock());
		JAWT_FreeDrawingSurface(drawingSurface, jawt.FreeDrawingSurface());
	}
}