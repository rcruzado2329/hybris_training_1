/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.acceleratorcms.component.renderer.impl;

import de.hybris.platform.acceleratorcms.component.renderer.CMSComponentRenderer;
import de.hybris.platform.cms2.model.contents.components.CMSParagraphComponentModel;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;

import org.apache.commons.lang.StringUtils;


/**
 * {@link CMSComponentRenderer} implementation handling {@link CMSParagraphComponentModel} instances.
 */
public class CMSParagraphComponentRenderer implements CMSComponentRenderer<CMSParagraphComponentModel>
{
	@Override
	public void renderComponent(final PageContext pageContext, final CMSParagraphComponentModel component)
			throws ServletException, IOException
	{
		// <div class="content">${content}</div>
		final JspWriter out = pageContext.getOut();

		out.write("<div class=\"content\">");
		out.write(component.getContent() == null ? StringUtils.EMPTY : component.getContent());
		out.write("</div>");
	}
}
