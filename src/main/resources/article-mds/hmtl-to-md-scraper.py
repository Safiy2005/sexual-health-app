import requests
from markdownify import markdownify as md
from bs4 import BeautifulSoup as bs

def scrape_page (url):
    headers = {'User-Agent':'Mozilla/5.0'}  # acts as a normal browser
    folder_path = "src/main/resources/article-mds/"
    
    try:
        response = requests.get(url, headers=headers)
        response.raise_for_status()     # raises an error if 404 or other web error
        
        soup = bs(response.text, 'html.parser') # makes a searchable 'soup' thing
        article_tag = soup.find('article') # get content of <article> what we need in nhs condition pages
        
        if article_tag:
            
            for service_link in article_tag.find_all('a', class_='nhsuk-action-link__link'):    # gets rid of links like "find a pharmacy"
                service_link.decompose()
            
            
            markdown_content = md(str(article_tag), heading_style="ATX", strip=['a', 'script', 'style'])    # ATX uses hashtags for headers, use this or tarans code will prob break
            
            # write the md content to file:
            condition_name = url.strip('/').split('/')[-1]  
            filename = f"nhsConditionPg-{condition_name}.md"
            
            with open(folder_path + filename, "w", encoding="utf-8") as file:
                file.write(markdown_content)
            
            print(f"Generated file: {filename}")
            
        else:
            print("No <article> tag found on this page. Skipped")
    
    except Exception as e:
        return f"Error: {e}"
    
