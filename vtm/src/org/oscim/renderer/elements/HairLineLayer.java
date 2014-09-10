package org.oscim.renderer.elements;

import static org.oscim.backend.GL20.GL_ELEMENT_ARRAY_BUFFER;
import static org.oscim.backend.GL20.GL_LINES;
import static org.oscim.backend.GL20.GL_SHORT;
import static org.oscim.backend.GL20.GL_UNSIGNED_SHORT;
import static org.oscim.renderer.MapRenderer.COORD_SCALE;

import org.oscim.core.GeometryBuffer;
import org.oscim.renderer.GLShader;
import org.oscim.renderer.GLState;
import org.oscim.renderer.GLUtils;
import org.oscim.renderer.GLViewport;
import org.oscim.theme.styles.LineStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HairLineLayer extends IndexedRenderElement {
	static final Logger log = LoggerFactory.getLogger(HairLineLayer.class);

	public LineStyle line;

	public HairLineLayer(int level) {
		super(RenderElement.HAIRLINE);
		this.level = level;
	}

	public void addLine(GeometryBuffer geom) {
		short id = (short) numVertices;

		float pts[] = geom.points;

		boolean poly = geom.isPoly();
		int inPos = 0;

		for (int i = 0, n = geom.index.length; i < n; i++) {
			int len = geom.index[i];
			if (len < 0)
				break;

			if (len < 4 || (poly && len < 6)) {
				inPos += len;
				continue;
			}

			int end = inPos + len;

			vertexItems.add((short) (pts[inPos++] * COORD_SCALE),
			                (short) (pts[inPos++] * COORD_SCALE));
			short first = id;

			indiceItems.add(id++);
			numIndices++;

			while (inPos < end) {

				vertexItems.add((short) (pts[inPos++] * COORD_SCALE),
				                (short) (pts[inPos++] * COORD_SCALE));

				indiceItems.add(id);
				numIndices++;

				if (inPos == end) {
					if (poly) {
						indiceItems.add(id);
						numIndices++;

						indiceItems.add(first);
						numIndices++;
					}
					id++;
					break;
				}
				indiceItems.add(id++);
				numIndices++;
			}

		}
		numVertices = id;
	}

	public static class Renderer {
		static Shader shader;

		static boolean init() {
			shader = new Shader("hairline");
			return true;
		}

		public static class Shader extends GLShader {
			int uMVP, uColor, uWidth, uScreen, aPos;

			Shader(String shaderFile) {
				if (!create(shaderFile))
					return;

				uMVP = getUniform("u_mvp");
				uColor = getUniform("u_color");
				uWidth = getUniform("u_width");
				uScreen = getUniform("u_screen");
				aPos = getAttrib("a_pos");
			}

			public void set(GLViewport v) {
				useProgram();
				GLState.enableVertexArrays(aPos, -1);

				v.mvp.setAsUniform(uMVP);

				GL.glUniform2f(uScreen, v.getWidth() / 2, v.getHeight() / 2);

				GL.glLineWidth(2);
			}
		}

		public static RenderElement draw(RenderElement l, GLViewport v) {
			GLState.blend(true);

			Shader s = shader;
			s.useProgram();
			GLState.enableVertexArrays(s.aPos, -1);

			v.mvp.setAsUniform(s.uMVP);

			GL.glUniform2f(s.uScreen, v.getWidth() / 2, v.getHeight() / 2);

			GL.glLineWidth(2);

			for (; l != null && l.type == HAIRLINE; l = l.next) {
				HairLineLayer ll = (HairLineLayer) l;

				if (ll.indicesVbo == null)
					continue;

				ll.indicesVbo.bind();

				GLUtils.setColor(s.uColor, ll.line.color, 1);

				GL.glVertexAttribPointer(s.aPos, 2, GL_SHORT,
				                         false, 0, ll.offset);

				GL.glDrawElements(GL_LINES, ll.numIndices,
				                  GL_UNSIGNED_SHORT, 0);
			}
			GL.glLineWidth(1);

			GL.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);

			return l;
		}
	}
}
