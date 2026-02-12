import requests
from bs4 import BeautifulSoup
import google.generativeai as genai
import json
import os
import time
import concurrent.futures
import sys
import threading
import re
from urllib.parse import urlparse
from google.api_core import exceptions

# --- CONFIGURATION ---
GEMINI_API_KEY = input("Paste an API key: ")  # <--- PASTE YOUR KEY HERE
genai.configure(api_key=GEMINI_API_KEY)

model = genai.GenerativeModel('gemini-3-flash-preview')

# THREAD SETTINGS
MAX_WORKERS = 10  # Adjust based on your tier

# --- PATH FIX ---
# Get the absolute path of the folder where this script is located
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))

# Output to parent directory (article-mds/)
OUTPUT_DIR = os.path.join(SCRIPT_DIR, "..") 

# Look for urls.txt inside the SAME folder as the script
URLS_FILE = os.path.join(SCRIPT_DIR, "urls.txt")

METADATA_FILE = os.path.join(OUTPUT_DIR, "articles-metadata.json")

# GLOBAL SHARED DATA
articles_metadata = {}
metadata_lock = threading.Lock()

# --- SAFETY SETTINGS ---
safety_config = [
    {"category": "HARM_CATEGORY_HARASSMENT", "threshold": "BLOCK_NONE"},
    {"category": "HARM_CATEGORY_HATE_SPEECH", "threshold": "BLOCK_NONE"},
    {"category": "HARM_CATEGORY_SEXUALLY_EXPLICIT", "threshold": "BLOCK_NONE"},
    {"category": "HARM_CATEGORY_DANGEROUS_CONTENT", "threshold": "BLOCK_NONE"},
]

def scrape_text(url):
    try:
        headers = {'User-Agent': 'Mozilla/5.0'}
        response = requests.get(url, headers=headers)
        response.raise_for_status()
        soup = BeautifulSoup(response.text, 'html.parser')
        
        for element in soup(['script', 'style', 'nav', 'footer', 'header', 'aside', 'form']):
            element.decompose()
            
        text = soup.get_text(separator=' ')
        return ' '.join(text.split())
    except Exception as e:
        return None

def generate_content_with_retry(text, url, forced_filename, max_retries=3):
    """Sends text to Gemini with auto-retry."""
      
    prompt = f"""
    You are a technical writer converting web content into structured markdown files.
    
    SOURCE URL: {url}
    TARGET FILENAME: {forced_filename}
    RAW CONTENT:
    {text}

    INSTRUCTIONS:
    1. Rewrite the content into clean Markdown. 
       - Use a level 1 Header (#) for the main title.
       - Use level 2 Headers (##) for sections.
       - Use bullet points for lists.
       - remove the parts that mention brook, and add a small disclaimer to the summary that states the content is from brook.org
       - remove all links and text related to these links
       - Do not include embeds to other reccomended pages.
       - There should be a level 2 header for the summary at the top after the main title
       - Format what would be a level 2 subheader as a bullet under the level 2 heading with the title as the bold text next to the content
       - Do not change the wording of any of the content and headings, they must appear exactly as in the webpage on the h2 text, not the start of the section, there must be no summarising
       - There should not be singular bullets, they should just be text as bullets should only be among other bullets
       - Chop long sections and paragraphs so that they are digestable without rewording or summarising
       - for long storys or long text sections, break them into parts with level 2 headers saying "part 1" or "section name: part 2" etc so that the app formats them correctly.
       - these sections should be about 100 words and split at logical points.
       - section header names should remain concise or the original heading content
       - format the summary at the top like this:
        ## Summary
            *   **Overview** [Summary content].
            *   **Disclaimer** The following content is from [source].
       
    2. At the very bottom, append a JSON object strictly following this format. 
       - The key MUST be "{forced_filename}".
       - "source" should be "Brook".
       - "filePath" must be "article-mds/{forced_filename}".
       - Generate exactly 3-5 tags for this article,
       You MUST select them ONLY from this allowed list for these tags for consitency:
       [
           "Contraception",
           "STIs",
           "Pregnancy & Choices",
           "Relationships & Consent",
           "Anatomy & Cycles",
           "Mental Health & Wellbeing",
           "Sex & Pleasure",
           "Digital Life",
           "LGBTQ+",
           "Trans & Non-Binary",
           "Neurodivergent",
           "Penis & Testicles",
           "Vulva, Vagina & Uterus",
           "Everyone"
        ]
        - Ensure that the tags used are definitely relevant for the article
        - Generate at least 10 keywords for this article, these are to help a semantic search correctly identify the article as relevant
    
    REQUIRED JSON FORMAT:
    {{
        "{forced_filename}": {{
            "title": "The Title",
            "source": "Brook",
            "filePath": "article-mds/{forced_filename}",
            "tags": ["tag1", "tag2"],
            "keywords": ["key1", "key2", "key3"]
        }}
    }}
    
    Output ONLY the markdown followed by the JSON.
    """


    for attempt in range(max_retries):
        try:
            response = model.generate_content(prompt, safety_settings=safety_config)
            return response.text
        except exceptions.ResourceExhausted:
            print(f"⚠️ Rate Limit on {forced_filename}. Waiting 15s...")
            time.sleep(15)
        except Exception as e:
            print(f"!! Error on {url}: {e}")
            return None
    return None

def parse_and_save(llm_response, filename):
    """Separates MD from JSON, saves MD file, updates global JSON dict."""
    if not llm_response:
        return

    # Regex to find the JSON block at the end
    json_match = re.search(r"```json\s*(\{.*?\})\s*```", llm_response, re.DOTALL)
    
    markdown_content = llm_response
    metadata = None

    if json_match:
        json_str = json_match.group(1)
        try:
            metadata = json.loads(json_str)
            # Remove the JSON block from the markdown content
            markdown_content = llm_response.replace(json_match.group(0), "").strip()
        except json.JSONDecodeError:
            print(f"⚠️ JSON Decode Error for {filename}")

    # Save Markdown File
    try:
        filepath = os.path.join(OUTPUT_DIR, filename)
        with open(filepath, "w", encoding="utf-8") as f:
            f.write(markdown_content)
        print(f"✅ Saved MD: {filename}")
    except Exception as e:
        print(f"Error saving MD {filename}: {e}")

    # Update Global Metadata Dictionary
    if metadata:
        with metadata_lock:
            articles_metadata.update(metadata)
            print(f"Start Metadata update for {filename}")

def process_single_url(url):
    """Worker function."""
    parsed = urlparse(url)
    slug = [p for p in parsed.path.split("/") if p][-1] or "index"
    
    # Clean filename
    slug = re.sub(r'[^\w\-]', '', slug)
    filename = f"brook-{slug}.md"
    
    filepath = os.path.join(OUTPUT_DIR, filename)
    if os.path.exists(filepath):
        print(f"⏭️  Skipping {filename} (Exists)")
        return

    print(f"Processing: {filename}...")
    
    raw_text = scrape_text(url)
    if raw_text and len(raw_text) > 200:
        llm_response = generate_content_with_retry(raw_text, url, filename)
        if llm_response:
            parse_and_save(llm_response, filename)
    else:
        print(f"⚠️ Low content: {filename}")

def save_metadata_file():
    """Writes the aggregated metadata to the JSON file."""
    try:
        with open(METADATA_FILE, "w", encoding="utf-8") as f:
            json.dump(articles_metadata, f, indent=2)
        print(f"\n💾 Metadata saved to {METADATA_FILE}")
    except Exception as e:
        print(f"Error saving metadata file: {e}")

# --- MAIN EXECUTION ---
if __name__ == "__main__":
    if not os.path.exists(URLS_FILE):
        print(f"Error: '{URLS_FILE}' not found in current directory.")
        sys.exit()

    with open(URLS_FILE, "r") as f:
        urls = [line.strip() for line in f if line.strip()]

    print(f"Found {len(urls)} URLs.")
    print(f"Output Directory: {os.path.abspath(OUTPUT_DIR)}")
    print(f"🚀 Starting {MAX_WORKERS} threads.")
    print("🔴 PRESS CTRL+C TO STOP SAFELY.\n")

    executor = concurrent.futures.ThreadPoolExecutor(max_workers=MAX_WORKERS)
    
    try:
        futures = [executor.submit(process_single_url, url) for url in urls]
        for future in concurrent.futures.as_completed(futures):
            pass  # Wait for completion
            
    except KeyboardInterrupt:
        print("\n\n🛑 KILL SWITCH ACTIVATED!")
        executor.shutdown(wait=False, cancel_futures=True)
    
    finally:
        # Always save metadata, even if interrupted
        save_metadata_file()
        print("\n--- All Done ---")